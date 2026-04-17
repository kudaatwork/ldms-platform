use crate::errors::StorageError;
use crate::models::{DeletedMarker, FileMetadata, MetaSidecar, StoredFile};
use crate::storage::traits::FileStorage;
use async_trait::async_trait;
use chrono::{DateTime, Utc};
use sha2::{Digest, Sha256};
use std::path::PathBuf;
use tokio::fs;

pub struct LocalFileStorage {
    base_path: PathBuf,
}

impl LocalFileStorage {
    pub fn new(base_path: impl Into<PathBuf>) -> Self {
        Self {
            base_path: base_path.into(),
        }
    }

    fn object_path(&self, key: &str) -> PathBuf {
        self.base_path.join(key)
    }

    fn meta_path_buf(&self, key: &str) -> PathBuf {
        self.base_path.join(format!("{key}.meta"))
    }

    fn deleted_path_buf(&self, key: &str) -> PathBuf {
        self.base_path.join(format!("{key}.deleted"))
    }
}

#[async_trait]
impl FileStorage for LocalFileStorage {
    async fn store(
        &self,
        key: &str,
        bytes: &[u8],
        content_type: &str,
        original_name: &str,
    ) -> Result<StoredFile, StorageError> {
        let path = self.object_path(key);
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent).await?;
        }

        fs::write(&path, bytes).await?;

        let mut hasher = Sha256::new();
        hasher.update(bytes);
        let hash = format!("sha256:{:x}", hasher.finalize());
        let stored_at = Utc::now();
        let size_bytes = bytes.len() as u64;

        let meta = MetaSidecar {
            content_type: content_type.to_string(),
            original_file_name: original_name.to_string(),
            file_hash: hash.clone(),
            size_bytes,
            stored_at,
        };

        let meta_json = serde_json::to_string_pretty(&meta)
            .map_err(|e| StorageError::internal(e.to_string()))?;
        fs::write(self.meta_path_buf(key), meta_json.as_bytes()).await?;

        Ok(StoredFile {
            file_key: key.to_string(),
            file_name: original_name.to_string(),
            content_type: content_type.to_string(),
            size_bytes,
            file_hash: hash,
            stored_at,
        })
    }

    async fn retrieve(&self, key: &str) -> Result<(Vec<u8>, String), StorageError> {
        if self.is_deleted(key).await? {
            return Err(StorageError::Deleted);
        }
        let path = self.object_path(key);
        if !fs::try_exists(&path).await.unwrap_or(false) {
            return Err(StorageError::NotFound);
        }
        let meta_raw = fs::read_to_string(self.meta_path_buf(key)).await?;
        let meta: MetaSidecar = serde_json::from_str(&meta_raw)
            .map_err(|e| StorageError::internal(format!("corrupt meta: {e}")))?;
        let bytes = fs::read(&path).await?;
        Ok((bytes, meta.content_type))
    }

    async fn soft_delete(
        &self,
        key: &str,
        deleted_by: &str,
    ) -> Result<DateTime<Utc>, StorageError> {
        if !self.exists(key).await? {
            return Err(StorageError::NotFound);
        }
        let deleted_at = Utc::now();
        let marker = DeletedMarker {
            deleted_at,
            deleted_by: deleted_by.to_string(),
        };
        let json = serde_json::to_string_pretty(&marker)
            .map_err(|e| StorageError::internal(e.to_string()))?;
        fs::write(self.deleted_path_buf(key), json.as_bytes()).await?;
        Ok(deleted_at)
    }

    async fn metadata(&self, key: &str) -> Result<FileMetadata, StorageError> {
        let is_deleted = self.is_deleted(key).await?;
        let path = self.object_path(key);
        if !fs::try_exists(&path).await.unwrap_or(false) {
            return Err(StorageError::NotFound);
        }
        let meta_raw = fs::read_to_string(self.meta_path_buf(key)).await?;
        let meta: MetaSidecar = serde_json::from_str(&meta_raw)
            .map_err(|e| StorageError::internal(format!("corrupt meta: {e}")))?;
        let len = fs::metadata(&path).await?.len();
        Ok(FileMetadata {
            file_key: key.to_string(),
            file_name: meta.original_file_name,
            content_type: meta.content_type,
            size_bytes: len,
            file_hash: meta.file_hash,
            stored_at: meta.stored_at,
            is_deleted,
        })
    }

    async fn exists(&self, key: &str) -> Result<bool, StorageError> {
        let path = self.object_path(key);
        Ok(fs::try_exists(&path).await.unwrap_or(false))
    }

    async fn is_deleted(&self, key: &str) -> Result<bool, StorageError> {
        let p = self.deleted_path_buf(key);
        Ok(fs::try_exists(&p).await.unwrap_or(false))
    }

    fn display_base_path(&self) -> String {
        self.base_path.display().to_string()
    }

    fn backend_name(&self) -> &'static str {
        "local"
    }
}
