use crate::handlers::validation::{file_key, validate_path_segment};
use crate::state::AppState;
use actix_web::http::header::{CACHE_CONTROL, CONTENT_TYPE};
use actix_web::web::{Data, Path};
use actix_web::{HttpResponse, Result};

#[derive(serde::Deserialize)]
pub struct FilePath {
    org_id: String,
    ref_type: String,
    file_id: String,
}

pub async fn download_file(
    state: Data<AppState>,
    path: Path<FilePath>,
) -> Result<HttpResponse, actix_web::Error> {
    validate_path_segment(&path.org_id, "orgId")?;
    validate_path_segment(&path.ref_type, "refType")?;
    validate_path_segment(&path.file_id, "fileId")?;

    let key = file_key(&path.org_id, &path.ref_type, &path.file_id);
    let (bytes, content_type) = state.storage.retrieve(&key).await?;

    Ok(HttpResponse::Ok()
        .insert_header((CONTENT_TYPE, content_type))
        .insert_header((CACHE_CONTROL, "private, max-age=3600"))
        .body(bytes))
}
