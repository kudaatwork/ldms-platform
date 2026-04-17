use actix_web::{HttpResponse, ResponseError};
use serde::Serialize;

#[derive(Debug, Serialize)]
pub struct ErrorBody {
    pub error: String,
    pub message: String,
}

#[derive(Debug, thiserror::Error)]
pub enum AuthError {
    #[error("Invalid or missing internal token")]
    Forbidden,
}

impl ResponseError for AuthError {
    fn error_response(&self) -> HttpResponse {
        HttpResponse::Forbidden().json(ErrorBody {
            error: "Forbidden".to_string(),
            message: self.to_string(),
        })
    }
}

#[derive(Debug, thiserror::Error)]
pub enum ValidationError {
    #[error("{0}")]
    Message(String),
}

impl ResponseError for ValidationError {
    fn error_response(&self) -> HttpResponse {
        HttpResponse::BadRequest().json(ErrorBody {
            error: "ValidationError".to_string(),
            message: self.to_string(),
        })
    }
}

#[derive(Debug, thiserror::Error)]
pub enum StorageError {
    #[error("File not found")]
    NotFound,
    #[error("File has been deleted")]
    Deleted,
    #[error("Payload too large")]
    FileTooLarge,
    #[error("Invalid storage key")]
    InvalidKey,
    #[error("Storage I/O error: {0}")]
    Io(#[from] std::io::Error),
    #[error("Internal storage error: {0}")]
    Internal(String),
}

impl ResponseError for StorageError {
    fn error_response(&self) -> HttpResponse {
        match self {
            StorageError::NotFound => HttpResponse::NotFound().json(ErrorBody {
                error: "NotFound".to_string(),
                message: "File not found".to_string(),
            }),
            StorageError::Deleted => HttpResponse::NotFound().json(ErrorBody {
                error: "NotFound".to_string(),
                message: "File has been deleted".to_string(),
            }),
            StorageError::FileTooLarge => HttpResponse::PayloadTooLarge().json(ErrorBody {
                error: "PayloadTooLarge".to_string(),
                message: self.to_string(),
            }),
            StorageError::InvalidKey => HttpResponse::BadRequest().json(ErrorBody {
                error: "ValidationError".to_string(),
                message: self.to_string(),
            }),
            StorageError::Io(e) => HttpResponse::InternalServerError().json(ErrorBody {
                error: "Internal".to_string(),
                message: format!("Storage I/O error: {e}"),
            }),
            StorageError::Internal(msg) => HttpResponse::InternalServerError().json(ErrorBody {
                error: "Internal".to_string(),
                message: msg.clone(),
            }),
        }
    }
}

impl StorageError {
    pub fn internal(msg: impl Into<String>) -> Self {
        StorageError::Internal(msg.into())
    }
}
