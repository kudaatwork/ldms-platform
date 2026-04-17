use crate::storage::traits::FileStorage;
use std::sync::Arc;

pub struct AppState {
    pub storage: Arc<dyn FileStorage>,
    pub internal_token: String,
    pub max_file_bytes: usize,
}
