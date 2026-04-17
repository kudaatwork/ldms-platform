use crate::models::HealthResponse;
use crate::state::AppState;
use actix_web::web::Data;
use actix_web::{HttpResponse, Result};

pub async fn health(state: Data<AppState>) -> Result<HttpResponse, actix_web::Error> {
    let body = HealthResponse {
        status: "UP",
        service: "ldms-file-service",
        version: "1.0.0",
        storage_path: state.storage.display_base_path(),
        storage_backend: state.storage.backend_name().to_string(),
    };
    Ok(HttpResponse::Ok().json(body))
}
