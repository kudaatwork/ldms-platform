use anyhow::{Context, Result};
use serde::Deserialize;
use std::env;
use std::path::Path;

#[derive(Debug, Clone, Deserialize)]
pub struct ServerConfig {
    pub host: String,
    pub port: u16,
}

#[derive(Debug, Clone, Deserialize)]
pub struct StorageConfig {
    pub backend: String,
    pub base_path: String,
    pub max_file_size_mb: u64,
}

#[derive(Debug, Clone, Deserialize)]
pub struct SecurityConfig {
    pub internal_token: String,
}

#[derive(Debug, Clone, Deserialize)]
pub struct EurekaConfig {
    pub enabled: bool,
    pub url: String,
    pub service_name: String,
    pub heartbeat_interval_secs: u64,
}

#[derive(Debug, Clone, Deserialize)]
pub struct AppConfig {
    pub server: ServerConfig,
    pub storage: StorageConfig,
    pub security: SecurityConfig,
    pub eureka: EurekaConfig,
}

impl AppConfig {
    pub fn load() -> Result<Self> {
        let mut builder = config::Config::builder();

        if Path::new("config.toml").exists() {
            builder = builder.add_source(config::File::with_name("config.toml"));
        } else {
            builder = builder.add_source(
                config::File::from_str(include_str!("../config.toml"), config::FileFormat::Toml)
                    .required(true),
            );
        }

        let mut cfg: AppConfig = builder
            .build()
            .context("failed to build config")?
            .try_deserialize()
            .context("failed to deserialize config")?;

        cfg.apply_env_overrides();
        Ok(cfg)
    }

    fn apply_env_overrides(&mut self) {
        if let Ok(v) = env::var("SERVER_HOST") {
            if !v.is_empty() {
                self.server.host = v;
            }
        }
        if let Ok(v) = env::var("SERVER_PORT") {
            if let Ok(p) = v.parse::<u16>() {
                self.server.port = p;
            }
        }
        if let Ok(v) = env::var("STORAGE_BACKEND") {
            if !v.is_empty() {
                self.storage.backend = v;
            }
        }
        if let Ok(v) = env::var("BASE_PATH") {
            if !v.is_empty() {
                self.storage.base_path = v;
            }
        }
        if let Ok(v) = env::var("MAX_FILE_SIZE_MB") {
            if let Ok(n) = v.parse::<u64>() {
                self.storage.max_file_size_mb = n;
            }
        }
        if let Ok(v) = env::var("INTERNAL_TOKEN") {
            if !v.is_empty() {
                self.security.internal_token = v;
            }
        }
        if let Ok(v) = env::var("EUREKA_ENABLED") {
            if let Ok(b) = v.parse::<bool>() {
                self.eureka.enabled = b;
            } else if v.eq_ignore_ascii_case("true") || v == "1" {
                self.eureka.enabled = true;
            } else if v.eq_ignore_ascii_case("false") || v == "0" {
                self.eureka.enabled = false;
            }
        }
        if let Ok(v) = env::var("EUREKA_URL") {
            if !v.is_empty() {
                self.eureka.url = v.trim_end_matches('/').to_string();
            }
        }
    }

    pub fn max_file_bytes(&self) -> usize {
        (self.storage.max_file_size_mb.saturating_mul(1024 * 1024)) as usize
    }
}
