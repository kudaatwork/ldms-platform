use crate::errors::{AuthError, ErrorBody};
use crate::state::AppState;
use actix_web::body::BoxBody;
use actix_web::dev::{ServiceRequest, ServiceResponse};
use actix_web::http::Method;
use actix_web::middleware::Next;
use actix_web::web::Data;
use actix_web::{Error, HttpResponse, ResponseError};

/// Paths that must work without `X-Internal-Token` (browser-friendly).
fn is_public_browse_path(method: &Method, path: &str) -> bool {
    if *method != Method::GET {
        return false;
    }
    matches!(path, "/" | "/ui" | "/favicon.ico")
}

pub async fn internal_token_middleware(
    req: ServiceRequest,
    next: Next<BoxBody>,
) -> Result<ServiceResponse<BoxBody>, Error> {
    let path = req.path().to_string();
    if is_public_browse_path(req.method(), path.as_str()) {
        return next.call(req).await;
    }

    let state = req
        .app_data::<Data<AppState>>()
        .map(|d| d.get_ref().internal_token.clone());

    let Some(expected) = state else {
        let (req, _) = req.into_parts();
        let res = HttpResponse::InternalServerError().json(ErrorBody {
            error: "Internal".to_string(),
            message: "Application state not configured".to_string(),
        });
        return Ok(ServiceResponse::new(req, res.map_into_boxed_body()));
    };

    let token_ok = req
        .headers()
        .get("X-Internal-Token")
        .and_then(|v| v.to_str().ok())
        .map(|t| t == expected.as_str())
        .unwrap_or(false);

    if !token_ok {
        let (req, _) = req.into_parts();
        let res = AuthError::Forbidden.error_response();
        return Ok(ServiceResponse::new(req, res.map_into_boxed_body()));
    }

    next.call(req).await
}
