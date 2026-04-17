mod config;
mod errors;
mod eureka;
mod handlers;
mod middleware;
mod models;
mod state;
mod storage;

use crate::config::AppConfig;
use crate::handlers::{
    delete::soft_delete_file,
    download::download_file,
    health::health,
    metadata::get_metadata,
    root::root,
    ui::ui,
    upload::upload_file,
};
use crate::middleware::auth::internal_token_middleware;
use crate::state::AppState;
use actix_web::middleware::from_fn;
use actix_web::web::Data;
use actix_web::{web, App, HttpServer};
use std::sync::Arc;
use tokio::signal;
use tracing_actix_web::TracingLogger;
use tracing_subscriber::prelude::*;
use tracing_subscriber::{fmt, EnvFilter};

#[tokio::main]
async fn main() -> std::io::Result<()> {
    dotenvy::dotenv().ok();
    init_tracing();

    let cfg = AppConfig::load().expect("failed to load configuration");
    let max_file_bytes = cfg.max_file_bytes();
    let bind_addr = format!("{}:{}", cfg.server.host, cfg.server.port);

    let storage = storage::create_storage(&cfg)
        .await
        .expect("failed to initialize storage");

    let eureka = if cfg.eureka.enabled {
        eureka::run_eureka_lifecycle(&cfg).await
    } else {
        None
    };

    let app_state = Data::new(AppState {
        storage,
        internal_token: cfg.security.internal_token.clone(),
        max_file_bytes,
    });

    let server = HttpServer::new(move || {
        App::new()
            .app_data(app_state.clone())
            .wrap(from_fn(internal_token_middleware))
            .wrap(TracingLogger::default())
            .route("/", web::get().to(root))
            .route("/ui", web::get().to(ui))
            .route("/health", web::get().to(health))
            .service(
                web::scope("/files")
                    .route("/upload", web::post().to(upload_file))
                    .route(
                        "/metadata/{org_id}/{ref_type}/{file_id}",
                        web::get().to(get_metadata),
                    )
                    .service(
                        web::resource("/{org_id}/{ref_type}/{file_id}")
                            .route(web::get().to(download_file))
                            .route(web::delete().to(soft_delete_file)),
                    ),
            )
    })
    .bind(&bind_addr)?
    .shutdown_timeout(30)
    .run();

    let handle = server.handle();
    let server_task = tokio::spawn(server);

    tracing::info!(%bind_addr, "ldms-file-service listening");

    shutdown_signal().await;

    tracing::info!("shutdown signal received, draining connections");

    if let Some(client) = eureka.as_ref() {
        client.shutdown_notify().notify_waiters();
        client.deregister().await;
    }

    handle.stop(true).await;

    match server_task.await {
        Ok(Ok(())) => {}
        Ok(Err(e)) => return Err(e),
        Err(e) => {
            return Err(std::io::Error::new(
                std::io::ErrorKind::Other,
                format!("server task join error: {e}"),
            ));
        }
    }

    tracing::info!("ldms-file-service stopped");
    Ok(())
}

fn init_tracing() {
    let env_filter = EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info"));

    let fmt_layer = fmt::layer().json().with_target(true).with_current_span(true);

    tracing_subscriber::registry()
        .with(env_filter)
        .with(fmt_layer)
        .init();
}

async fn shutdown_signal() {
    let ctrl_c = async {
        signal::ctrl_c()
            .await
            .expect("failed to install Ctrl+C handler");
    };

    #[cfg(unix)]
    let terminate = async {
        signal::unix::signal(signal::unix::SignalKind::terminate())
            .expect("failed to install SIGTERM handler")
            .recv()
            .await;
    };

    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        _ = ctrl_c => {},
        _ = terminate => {},
    }
}
