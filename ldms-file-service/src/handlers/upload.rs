use crate::errors::{StorageError, ValidationError};
use crate::handlers::validation::{
    file_key, validate_organization_id, validate_reference_type,
};
use crate::models::UploadResponse;
use crate::state::AppState;
use actix_multipart::Multipart;
use actix_web::web::Data;
use actix_web::{HttpResponse, Result};
use futures_util::TryStreamExt;
use mime::Mime;
use std::borrow::Cow;
use std::path::Path;
use tracing::{info_span, Instrument};
use uuid::Uuid;

fn extension_from_original_or_mime(original: Option<&str>, content_type: &str) -> String {
    if let Some(name) = original {
        if let Some(ext) = Path::new(name).extension().and_then(|e| e.to_str()) {
            if !ext.is_empty() {
                return ext.to_lowercase();
            }
        }
    }
    if let Ok(m) = content_type.parse::<Mime>() {
        if let Some(extensions) = mime_guess::get_mime_extensions(&m) {
            if let Some(ext) = extensions.first() {
                return (*ext).to_ascii_lowercase();
            }
        }
    }
    "bin".to_string()
}

pub async fn upload_file(
    state: Data<AppState>,
    mut payload: Multipart,
) -> Result<HttpResponse, actix_web::Error> {
    let max = state.max_file_bytes;

    let mut organization_id: Option<String> = None;
    let mut reference_type: Option<String> = None;
    let mut original_file_name: Option<String> = None;
    let mut file_bytes: Option<Vec<u8>> = None;
    let mut file_content_type: String = "application/octet-stream".to_string();

    while let Some(mut field) = payload.try_next().await? {
        let field_name = field
            .content_disposition()
            .and_then(|cd| cd.get_name())
            .unwrap_or_default()
            .to_string();

        match field_name.as_str() {
            "organizationId" => {
                let mut bytes = Vec::new();
                while let Some(chunk) = field.try_next().await? {
                    bytes.extend_from_slice(&chunk);
                    if bytes.len() > 4096 {
                        return Err(ValidationError::Message("organizationId too long".into()).into());
                    }
                }
                organization_id = Some(String::from_utf8_lossy(&bytes).trim().to_string());
            }
            "referenceType" => {
                let mut bytes = Vec::new();
                while let Some(chunk) = field.try_next().await? {
                    bytes.extend_from_slice(&chunk);
                    if bytes.len() > 512 {
                        return Err(ValidationError::Message("referenceType too long".into()).into());
                    }
                }
                reference_type = Some(String::from_utf8_lossy(&bytes).trim().to_string());
            }
            "originalFileName" => {
                let mut bytes = Vec::new();
                while let Some(chunk) = field.try_next().await? {
                    bytes.extend_from_slice(&chunk);
                    if bytes.len() > 1024 {
                        return Err(ValidationError::Message("originalFileName too long".into()).into());
                    }
                }
                let s = String::from_utf8_lossy(&bytes).trim().to_string();
                if !s.is_empty() {
                    original_file_name = Some(s);
                }
            }
            "file" => {
                if let Some(ct) = field.content_type() {
                    file_content_type = ct.to_string();
                }
                let mut buf = Vec::new();
                while let Some(chunk) = field.try_next().await? {
                    buf.extend_from_slice(&chunk);
                    if buf.len() > max {
                        return Err(StorageError::FileTooLarge.into());
                    }
                }
                file_bytes = Some(buf);
            }
            _ => {
                while let Some(_chunk) = field.try_next().await? {}
            }
        }
    }

    let organization_id = organization_id
        .filter(|s| !s.is_empty())
        .ok_or_else(|| ValidationError::Message("organizationId is required".into()))?;
    let reference_type = reference_type
        .filter(|s| !s.is_empty())
        .ok_or_else(|| ValidationError::Message("referenceType is required".into()))?;
    let file_bytes = file_bytes.ok_or_else(|| ValidationError::Message("file field is required".into()))?;

    validate_organization_id(&organization_id)?;
    validate_reference_type(&reference_type)?;

    if file_bytes.is_empty() {
        return Err(ValidationError::Message("file must not be empty".into()).into());
    }

    let orig_display: Cow<'_, str> = original_file_name
        .as_deref()
        .map(Cow::Borrowed)
        .unwrap_or(Cow::Borrowed("upload.bin"));

    let ext = extension_from_original_or_mime(original_file_name.as_deref(), &file_content_type);
    let id = Uuid::new_v4();
    let file_id = format!("{id}.{ext}");
    let key = file_key(&organization_id, &reference_type, &file_id);

    let span = info_span!(
        "file_upload",
        file_key = %key,
        content_type = %file_content_type,
        size_bytes = file_bytes.len(),
        organization_id = %organization_id,
        reference_type = %reference_type,
    );

    let stored = async {
        state
            .storage
            .store(
                &key,
                &file_bytes,
                &file_content_type,
                orig_display.as_ref(),
            )
            .await
    }
    .instrument(span.clone())
    .await?;

    tracing::info!(
        parent: &span,
        file_key = %stored.file_key,
        content_type = %stored.content_type,
        size_bytes = stored.size_bytes,
        organization_id = %organization_id,
        reference_type = %reference_type,
        file_hash = %stored.file_hash,
        "file uploaded"
    );

    let body = UploadResponse {
        file_key: stored.file_key,
        file_name: stored.file_name,
        content_type: stored.content_type,
        size_bytes: stored.size_bytes,
        file_hash: stored.file_hash,
        stored_at: stored
            .stored_at
            .to_rfc3339_opts(chrono::SecondsFormat::Secs, true),
    };

    Ok(HttpResponse::Created().json(body))
}
