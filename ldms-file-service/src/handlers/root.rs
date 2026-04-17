use actix_web::{HttpResponse, Responder};
use serde::Serialize;

#[derive(Serialize)]
pub struct RootInfo {
    pub service: &'static str,
    pub message: &'static str,
    pub docs: RootDocs,
}

#[derive(Serialize)]
pub struct RootDocs {
    pub ui: &'static str,
    pub health: &'static str,
    pub files_api: &'static str,
    pub auth_header: &'static str,
}

pub async fn root() -> impl Responder {
    HttpResponse::Ok().json(RootInfo {
        service: "ldms-file-service",
        message: "Send header X-Internal-Token on /health and all /files/* routes.",
        docs: RootDocs {
            ui: "GET /ui (browser UI)",
            health: "GET /health",
            files_api: "/files/upload, /files/{org_id}/{ref_type}/{file_id}, …",
            auth_header: "X-Internal-Token: <configured internal token>",
        },
    })
}
