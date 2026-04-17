pub mod local;
pub mod traits;

#[cfg(feature = "s3")]
pub mod s3;

use crate::config::AppConfig;
use crate::errors::StorageError;
use crate::storage::local::LocalFileStorage;
use crate::storage::traits::FileStorage;
use std::path::PathBuf;
use std::sync::Arc;

pub async fn create_storage(cfg: &AppConfig) -> Result<Arc<dyn FileStorage>, StorageError> {
    let backend = cfg.storage.backend.to_lowercase();
    match backend.as_str() {
        "local" => {
            let base = PathBuf::from(&cfg.storage.base_path);
            Ok(Arc::new(LocalFileStorage::new(base)))
        }
        "s3" => {
            #[cfg(feature = "s3")]
            {
                let bucket = std::env::var("S3_BUCKET").map_err(|_| {
                    StorageError::internal("S3_BUCKET environment variable is required for S3 backend")
                })?;
                let endpoint = std::env::var("S3_ENDPOINT").ok().filter(|s| !s.is_empty());
                let region = std::env::var("S3_REGION").unwrap_or_else(|_| "us-east-1".to_string());
                let s3 = s3::S3FileStorage::new(bucket, endpoint, region)
                    .await
                    .map_err(|e| StorageError::internal(e.to_string()))?;
                Ok(Arc::new(s3))
            }
            #[cfg(not(feature = "s3"))]
            {
                Err(StorageError::internal(
                    "S3 backend requested but binary was built without `--features s3`",
                ))
            }
        }
        other => Err(StorageError::internal(format!(
            "Unknown storage backend: {other}"
        ))),
    }
}
