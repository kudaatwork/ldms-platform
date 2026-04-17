#![cfg(feature = "s3")]

use crate::errors::StorageError;
use crate::models::{FileMetadata, StoredFile};
use crate::storage::traits::FileStorage;
use async_trait::async_trait;
use aws_config::BehaviorVersion;
use aws_sdk_s3::primitives::ByteStream;
use aws_sdk_s3::types::{Tag, Tagging};
use aws_sdk_s3::Client;
use chrono::{DateTime, SecondsFormat, Utc};
use sha2::{Digest, Sha256};

pub struct S3FileStorage {
    client: Client,
    bucket: String,
}

impl S3FileStorage {
    pub async fn new(
        bucket: String,
        endpoint: Option<String>,
        region: String,
    ) -> Result<Self, StorageError> {
        let mut loader = aws_config::defaults(BehaviorVersion::latest());
        if !region.is_empty() {
            loader = loader.region(aws_config::Region::new(region));
        }
        let shared = loader.load().await;

        let mut s3_builder = aws_sdk_s3::config::Builder::from(&shared);
        if let Some(ref ep) = endpoint {
            if !ep.is_empty() {
                s3_builder = s3_builder.endpoint_url(ep).force_path_style(true);
            }
        }
        let client = Client::from_conf(s3_builder.build());
        Ok(Self { client, bucket })
    }

    async fn object_tags_include_deleted(&self, key: &str) -> Result<bool, StorageError> {
        let out = self
            .client
            .get_object_tagging()
            .bucket(&self.bucket)
            .key(key)
            .send()
            .await
            .map_err(|e| {
                let msg = e.to_string();
                if msg.contains("NoSuchKey") || msg.contains("NotFound") {
                    StorageError::NotFound
                } else {
                    StorageError::internal(msg)
                }
            })?;

        Ok(out.tag_set().iter().any(|t| {
            t.key().eq_ignore_ascii_case("deleted") && t.value().eq_ignore_ascii_case("true")
        }))
    }
}

#[async_trait]
impl FileStorage for S3FileStorage {
    async fn store(
        &self,
        key: &str,
        bytes: &[u8],
        content_type: &str,
        original_name: &str,
    ) -> Result<StoredFile, StorageError> {
        let mut hasher = Sha256::new();
        hasher.update(bytes);
        let hash = format!("sha256:{:x}", hasher.finalize());
        let stored_at = Utc::now();
        let stored_rfc = stored_at.to_rfc3339_opts(chrono::SecondsFormat::Secs, true);

        self.client
            .put_object()
            .bucket(&self.bucket)
            .key(key)
            .body(ByteStream::from(bytes.to_vec()))
            .content_type(content_type)
            .metadata("ldms-original-filename", original_name)
            .metadata("ldms-file-hash", &hash)
            .metadata("ldms-stored-at", &stored_rfc)
            .send()
            .await
            .map_err(|e| StorageError::internal(e.to_string()))?;

        Ok(StoredFile {
            file_key: key.to_string(),
            file_name: original_name.to_string(),
            content_type: content_type.to_string(),
            size_bytes: bytes.len() as u64,
            file_hash: hash,
            stored_at,
        })
    }

    async fn retrieve(&self, key: &str) -> Result<(Vec<u8>, String), StorageError> {
        if self.is_deleted(key).await? {
            return Err(StorageError::Deleted);
        }

        let out = self
            .client
            .get_object()
            .bucket(&self.bucket)
            .key(key)
            .send()
            .await
            .map_err(|e| {
                let m = e.to_string();
                if m.contains("NoSuchKey") || m.contains("NotFound") {
                    StorageError::NotFound
                } else {
                    StorageError::internal(m)
                }
            })?;

        let ct = out
            .content_type()
            .map(|s| s.to_string())
            .unwrap_or_else(|| "application/octet-stream".to_string());

        let data = out
            .body
            .collect()
            .await
            .map_err(|e| StorageError::internal(e.to_string()))?
            .into_bytes()
            .to_vec();

        Ok((data, ct))
    }

    async fn soft_delete(
        &self,
        key: &str,
        deleted_by: &str,
    ) -> Result<DateTime<Utc>, StorageError> {
        if !self.exists(key).await? {
            return Err(StorageError::NotFound);
        }

        let deleted_at_dt = Utc::now();
        let deleted_at = deleted_at_dt.to_rfc3339_opts(SecondsFormat::Secs, true);

        let tag_deleted = Tag::builder()
            .key("deleted")
            .value("true")
            .build()
            .map_err(|e| StorageError::internal(e.to_string()))?;
        let tag_by = Tag::builder()
            .key("deleted-by")
            .value(deleted_by)
            .build()
            .map_err(|e| StorageError::internal(e.to_string()))?;
        let tag_at = Tag::builder()
            .key("deleted-at")
            .value(&deleted_at)
            .build()
            .map_err(|e| StorageError::internal(e.to_string()))?;

        let tagging = Tagging::builder()
            .set_tag_set(Some(vec![tag_deleted, tag_by, tag_at]))
            .build()
            .map_err(|e| StorageError::internal(e.to_string()))?;

        self.client
            .put_object_tagging()
            .bucket(&self.bucket)
            .key(key)
            .tagging(tagging)
            .send()
            .await
            .map_err(|e| StorageError::internal(e.to_string()))?;

        Ok(deleted_at_dt)
    }

    async fn metadata(&self, key: &str) -> Result<FileMetadata, StorageError> {
        let is_deleted = self.is_deleted(key).await?;

        let head = self
            .client
            .head_object()
            .bucket(&self.bucket)
            .key(key)
            .send()
            .await
            .map_err(|e| {
                let m = e.to_string();
                if m.contains("NotFound") || m.contains("NoSuchKey") {
                    StorageError::NotFound
                } else {
                    StorageError::internal(m)
                }
            })?;

        let content_type = head
            .content_type()
            .map(|s| s.to_string())
            .unwrap_or_else(|| "application/octet-stream".to_string());

        let size_bytes = head.content_length().unwrap_or(0) as u64;

        let meta = head.metadata();
        let file_name = meta
            .and_then(|m| m.get("ldms-original-filename"))
            .cloned()
            .unwrap_or_default();
        let file_hash = meta
            .and_then(|m| m.get("ldms-file-hash"))
            .cloned()
            .unwrap_or_else(|| "sha256:".to_string());
        let stored_at = meta
            .and_then(|m| m.get("ldms-stored-at"))
            .and_then(|s| DateTime::parse_from_rfc3339(s).ok())
            .map(|d| d.with_timezone(&Utc))
            .unwrap_or_else(Utc::now);

        Ok(FileMetadata {
            file_key: key.to_string(),
            file_name,
            content_type,
            size_bytes,
            file_hash,
            stored_at,
            is_deleted,
        })
    }

    async fn exists(&self, key: &str) -> Result<bool, StorageError> {
        match self
            .client
            .head_object()
            .bucket(&self.bucket)
            .key(key)
            .send()
            .await
        {
            Ok(_) => Ok(true),
            Err(e) => {
                let m = e.to_string();
                if m.contains("NotFound") || m.contains("NoSuchKey") {
                    Ok(false)
                } else {
                    Err(StorageError::internal(m))
                }
            }
        }
    }

    async fn is_deleted(&self, key: &str) -> Result<bool, StorageError> {
        if !self.exists(key).await? {
            return Ok(false);
        }
        self.object_tags_include_deleted(key).await
    }

    fn display_base_path(&self) -> String {
        format!("s3://{}", self.bucket)
    }

    fn backend_name(&self) -> &'static str {
        "s3"
    }
}
