use crate::handlers::validation::{file_key, validate_path_segment};
use crate::models::DeleteResponse;
use crate::state::AppState;
use actix_web::web::{Data, Path};
use actix_web::{HttpRequest, HttpResponse, Result};

#[derive(serde::Deserialize)]
pub struct FilePath {
    org_id: String,
    ref_type: String,
    file_id: String,
}

pub async fn soft_delete_file(
    state: Data<AppState>,
    path: Path<FilePath>,
    req: HttpRequest,
) -> Result<HttpResponse, actix_web::Error> {
    validate_path_segment(&path.org_id, "orgId")?;
    validate_path_segment(&path.ref_type, "refType")?;
    validate_path_segment(&path.file_id, "fileId")?;

    let key = file_key(&path.org_id, &path.ref_type, &path.file_id);

    let deleted_by = req
        .headers()
        .get("X-Deleted-By")
        .and_then(|v| v.to_str().ok())
        .filter(|s| !s.is_empty())
        .unwrap_or("ldms-file-service");

    let deleted_at = state.storage.soft_delete(&key, deleted_by).await?;

    Ok(HttpResponse::Ok().json(DeleteResponse {
        file_key: key,
        deleted_at: deleted_at.to_rfc3339_opts(chrono::SecondsFormat::Secs, true),
    }))
}
