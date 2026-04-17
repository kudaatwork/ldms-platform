use crate::handlers::validation::{file_key, validate_path_segment};
use crate::models::MetadataResponse;
use crate::state::AppState;
use actix_web::web::{Data, Path};
use actix_web::{HttpResponse, Result};

#[derive(serde::Deserialize)]
pub struct FilePath {
    org_id: String,
    ref_type: String,
    file_id: String,
}

pub async fn get_metadata(
    state: Data<AppState>,
    path: Path<FilePath>,
) -> Result<HttpResponse, actix_web::Error> {
    validate_path_segment(&path.org_id, "orgId")?;
    validate_path_segment(&path.ref_type, "refType")?;
    validate_path_segment(&path.file_id, "fileId")?;

    let key = file_key(&path.org_id, &path.ref_type, &path.file_id);
    let meta = state.storage.metadata(&key).await?;

    Ok(HttpResponse::Ok().json(MetadataResponse {
        file_key: meta.file_key,
        file_name: meta.file_name,
        content_type: meta.content_type,
        size_bytes: meta.size_bytes,
        file_hash: meta.file_hash,
        stored_at: meta
            .stored_at
            .to_rfc3339_opts(chrono::SecondsFormat::Secs, true),
        is_deleted: meta.is_deleted,
    }))
}
