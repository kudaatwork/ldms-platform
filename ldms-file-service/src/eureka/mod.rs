use crate::config::AppConfig;
use reqwest::Client;
use serde_json::json;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::Notify;
use tokio::time::sleep;
use tracing::warn;

pub struct EurekaClient {
    http: Client,
    base_url: String,
    app_name: String,
    instance_id: String,
    hostname: String,
    ip_addr: String,
    port: u16,
    heartbeat_secs: u64,
    registered: AtomicBool,
    shutdown: Arc<Notify>,
}

impl EurekaClient {
    pub fn new(cfg: &AppConfig) -> Self {
        let hostname = hostname::get()
            .map(|h| h.to_string_lossy().to_string())
            .unwrap_or_else(|_| "localhost".to_string());
        let ip_addr = local_ip();
        let port = cfg.server.port;
        let instance_id = format!("{ip_addr}:{app}:{port}", app = cfg.eureka.service_name);

        let http = Client::builder()
            .timeout(Duration::from_secs(10))
            .build()
            .expect("reqwest client");

        Self {
            http,
            base_url: cfg.eureka.url.trim_end_matches('/').to_string(),
            app_name: cfg.eureka.service_name.clone(),
            instance_id,
            hostname,
            ip_addr,
            port,
            heartbeat_secs: cfg.eureka.heartbeat_interval_secs.max(5),
            registered: AtomicBool::new(false),
            shutdown: Arc::new(Notify::new()),
        }
    }

    fn apps_url(&self) -> String {
        format!("{}/apps/{}", self.base_url, self.app_name)
    }

    fn instance_url(&self) -> String {
        let enc = urlencoding::encode(&self.instance_id);
        format!("{}/{}", self.apps_url(), enc)
    }

    pub async fn register(&self) {
        let health_url = format!("http://{}:{}/health", self.ip_addr, self.port);
        let home_url = format!("http://{}:{}/", self.ip_addr, self.port);

        let body = json!({
            "instance": {
                "instanceId": self.instance_id,
                "hostName": self.hostname,
                "app": self.app_name,
                "ipAddr": self.ip_addr,
                "status": "UP",
                "overriddenstatus": "UNKNOWN",
                "port": { "$": self.port, "@enabled": "true" },
                "securePort": { "$": 443, "@enabled": "false" },
                "countryId": 1,
                "dataCenterInfo": {
                    "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                    "name": "MyOwn"
                },
                "leaseInfo": {
                    "renewalIntervalInSecs": self.heartbeat_secs,
                    "durationInSecs": 90,
                    "registrationTimestamp": 0,
                    "lastRenewalTimestamp": 0,
                    "evictionTimestamp": 0,
                    "serviceUpTimestamp": 0
                },
                "metadata": {},
                "homePageUrl": home_url,
                "statusPageUrl": health_url.clone(),
                "healthCheckUrl": health_url,
                "vipAddress": self.app_name,
                "secureVipAddress": self.app_name,
                "isCoordinatingDiscoveryServer": false,
                "lastUpdatedTimestamp": 0,
                "lastDirtyTimestamp": 0,
                "actionType": "ADDED"
            }
        });

        let url = self.apps_url();
        match self
            .http
            .post(&url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .json(&body)
            .send()
            .await
        {
            Ok(resp) => {
                if resp.status().is_success() {
                    tracing::info!(target: "ldms_file_service::eureka", %url, "Eureka registration succeeded");
                    self.registered.store(true, Ordering::SeqCst);
                } else {
                    let status = resp.status();
                    let text = resp.text().await.unwrap_or_default();
                    warn!(
                        target: "ldms_file_service::eureka",
                        %url,
                        %status,
                        body = %text,
                        "Eureka registration failed; continuing without discovery"
                    );
                }
            }
            Err(e) => {
                warn!(
                    target: "ldms_file_service::eureka",
                    %url,
                    error = %e,
                    "Eureka unreachable; continuing without discovery"
                );
            }
        }
    }

    pub async fn heartbeat(&self) {
        let url = self.instance_url();
        match self
            .http
            .put(&url)
            .header("Accept", "application/json")
            .send()
            .await
        {
            Ok(resp) => {
                if !resp.status().is_success() {
                    warn!(
                        target: "ldms_file_service::eureka",
                        %url,
                        status = %resp.status(),
                        "Eureka heartbeat non-success"
                    );
                }
            }
            Err(e) => {
                warn!(
                    target: "ldms_file_service::eureka",
                    %url,
                    error = %e,
                    "Eureka heartbeat failed"
                );
            }
        }
    }

    pub async fn deregister(&self) {
        if !self.registered.load(Ordering::SeqCst) {
            return;
        }
        let url = self.instance_url();
        match self
            .http
            .delete(&url)
            .header("Accept", "application/json")
            .send()
            .await
        {
            Ok(resp) => {
                if resp.status().is_success() {
                    tracing::info!(target: "ldms_file_service::eureka", %url, "Eureka deregistration succeeded");
                } else {
                    warn!(
                        target: "ldms_file_service::eureka",
                        %url,
                        status = %resp.status(),
                        "Eureka deregistration non-success"
                    );
                }
            }
            Err(e) => {
                warn!(
                    target: "ldms_file_service::eureka",
                    %url,
                    error = %e,
                    "Eureka deregister failed"
                );
            }
        }
        self.registered.store(false, Ordering::SeqCst);
    }

    pub fn shutdown_notify(&self) -> Arc<Notify> {
        Arc::clone(&self.shutdown)
    }

    pub fn spawn_heartbeat_loop(self: Arc<Self>) {
        tokio::spawn(async move {
            let interval = Duration::from_secs(self.heartbeat_secs);
            loop {
                tokio::select! {
                    _ = self.shutdown.notified() => {
                        break;
                    }
                    _ = sleep(interval) => {
                        if self.registered.load(Ordering::SeqCst) {
                            self.heartbeat().await;
                        }
                    }
                }
            }
        });
    }
}

fn local_ip() -> String {
    std::env::var("EUREKA_INSTANCE_IP")
        .ok()
        .filter(|s| !s.is_empty())
        .unwrap_or_else(|| "127.0.0.1".to_string())
}

pub async fn run_eureka_lifecycle(cfg: &AppConfig) -> Option<Arc<EurekaClient>> {
    if !cfg.eureka.enabled {
        return None;
    }

    let client = Arc::new(EurekaClient::new(cfg));
    client.register().await;
    let c = Arc::clone(&client);
    c.spawn_heartbeat_loop();
    Some(client)
}
