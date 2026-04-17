use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetaSidecar {
    pub content_type: String,
    pub original_file_name: String,
    pub file_hash: String,
    pub size_bytes: u64,
    pub stored_at: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DeletedMarker {
    pub deleted_at: DateTime<Utc>,
    pub deleted_by: String,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UploadResponse {
    pub file_key: String,
    pub file_name: String,
    pub content_type: String,
    pub size_bytes: u64,
    pub file_hash: String,
    pub stored_at: String,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct DeleteResponse {
    pub file_key: String,
    pub deleted_at: String,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataResponse {
    pub file_key: String,
    pub file_name: String,
    pub content_type: String,
    pub size_bytes: u64,
    pub file_hash: String,
    pub stored_at: String,
    pub is_deleted: bool,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct HealthResponse {
    pub status: &'static str,
    pub service: &'static str,
    pub version: &'static str,
    pub storage_path: String,
    pub storage_backend: String,
}

#[derive(Debug, Clone)]
pub struct StoredFile {
    pub file_key: String,
    pub file_name: String,
    pub content_type: String,
    pub size_bytes: u64,
    pub file_hash: String,
    pub stored_at: DateTime<Utc>,
}

#[derive(Debug, Clone)]
pub struct FileMetadata {
    pub file_key: String,
    pub file_name: String,
    pub content_type: String,
    pub size_bytes: u64,
    pub file_hash: String,
    pub stored_at: DateTime<Utc>,
    pub is_deleted: bool,
}
