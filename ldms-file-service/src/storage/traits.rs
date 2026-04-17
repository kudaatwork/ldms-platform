use crate::errors::StorageError;
use crate::models::{FileMetadata, StoredFile};
use async_trait::async_trait;
use chrono::{DateTime, Utc};

#[async_trait]
pub trait FileStorage: Send + Sync {
    async fn store(
        &self,
        key: &str,
        bytes: &[u8],
        content_type: &str,
        original_name: &str,
    ) -> Result<StoredFile, StorageError>;

    async fn retrieve(&self, key: &str) -> Result<(Vec<u8>, String), StorageError>;

    async fn soft_delete(
        &self,
        key: &str,
        deleted_by: &str,
    ) -> Result<DateTime<Utc>, StorageError>;

    async fn metadata(&self, key: &str) -> Result<FileMetadata, StorageError>;

    async fn exists(&self, key: &str) -> Result<bool, StorageError>;

    async fn is_deleted(&self, key: &str) -> Result<bool, StorageError>;

    fn display_base_path(&self) -> String;

    fn backend_name(&self) -> &'static str;
}
