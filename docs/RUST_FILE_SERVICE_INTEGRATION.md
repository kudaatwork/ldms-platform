# Rust File Service — integration (Config, Docker, Gateway, flows)

This document is the operational companion to the Rust file service (`ldms-file-service`, port **8200**), `RustFsClient` in `ldms-shared-library`, and the metadata-only Java **file-upload-service** (port **9016**).

## Architecture

- **Bytes**: JVM services that need direct storage use **`RustFsClient`** (upload / download / delete / metadata) against the Rust service. Requests include header **`X-Internal-Token`** (same value as Rust `INTERNAL_TOKEN` and Java `rustfs.internal-token`).
- **Metadata**: **`ldms-file-upload-service`** persists **`FileUpload`** rows in MySQL and exposes REST APIs that return **base64** content in **`FileUploadDto.fileContent`** where applicable.
- **Rust service** is **internal infrastructure**. It is **not** routed through the **API Gateway**, and the **Angular** app does **not** call it directly.

## 1. Config repository (`ldms-config-repo`)

Templates for services (including organization-management) live under **`docs/ldms-config-repo/`** — see **`docs/ldms-config-repo/README.md`**. Copy the YAML files into your real Git config repo (the URI configured in the Config Server, e.g. `LDMS_CONFIG_GIT_URI`).

| File | When it is loaded |
|------|-------------------|
| **`ldms-file-upload-service-dev.yml`** | `spring.cloud.config.name=ldms-file-upload-service`, profile `dev` (default for LDMS file-upload) |
| **`ldms-file-upload-service-prod.yml`** | Same application name, profile `prod` |
| **`file-upload-service-dev.yml`** / **`file-upload-service-prod.yml`** | Only if you set `spring.cloud.config.name=file-upload-service` |

Set **`RUSTFS_INTERNAL_TOKEN`** in each environment so Java and Rust agree.

**Production** `rustfs.base-url` uses the Docker Compose service hostname **`http://file-service:8200`** on **`ldms-network`**.

## 2. API Gateway — no route to Rust

Do **not** add a Spring Cloud Gateway route to port **8200**. Clients use existing Java file-upload (and future Documents Management) APIs; those services orchestrate Rust or return metadata + base64 as designed.

## 3. Future service integration (per system design)

### Documents Management Service (Phases B, D, E — primary file orchestrator)

When that service exists, add:

**`application.yml` (or config-repo profile):**

```yaml
rustfs:
  enabled: true
  base-url: ${RUSTFS_BASE_URL:http://localhost:8200}
  internal-token: ${RUSTFS_INTERNAL_TOKEN:dev-token-change-me}
```

**Main application class:**

```java
@Import(RustFsClientConfig.class)
```

Inject **`RustFsClient`** where the service performs byte operations on behalf of Inventory, Shipment, Fleet, etc.

### Other services (Inventory, Shipment, Fleet, …)

- **Do not** add RustFS config.
- They call **Documents Management Service** (Feign/clients unchanged at the edge); that service owns orchestration to Rust FS.

System flow references:

- Phase B: Inventory → Documents Management Service  
- Phase D: Shipment → Documents Management Service  
- Phase E: Fleet → Documents Management Service  

## 4. Docker Compose

The **`infrastructure/docker-compose.yml`** services **`minio`**, **`minio-setup`**, and **`file-service`**:

- Build context: **`../ldms-file-service`** (relative to `infrastructure/`); image is built with **`--features s3`**.
- **MinIO** provides S3-compatible storage; **`minio-setup`** creates the bucket named by **`S3_BUCKET`** (see **`infrastructure/.env.example`**).
- **`file-service`** uses **`STORAGE_BACKEND=s3`**, **`S3_ENDPOINT=http://minio:9000`**, and **`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`** (defaults align with **`MINIO_ROOT_*`**).
- **`EUREKA_ENABLED=false`** by default so the container starts without a Eureka container in this stack. Set **`EUREKA_ENABLED=true`** and ensure **`EUREKA_URL`** resolves (e.g. `http://eureka-server:8761/eureka`) when Eureka runs on **`ldms-network`**.
- **Healthcheck** uses **`curl`** with **`X-Internal-Token`** (required on **`GET /health`** and **`/files/*`**; **`GET /`** is public).

Rebuild the image after Dockerfile changes: `docker compose build file-service`.

## 5. Migration strategy (existing LOCAL files)

Records with **`StorageProvider.LOCAL`** keep being read from **`file.upload.location`** on disk. New uploads use **`RUST_FS`**. No bulk data migration is required.

## 6. Suggested startup order (local / full stack)

1. **Config Server** (8888)  
2. **Eureka Server** (8761)  
3. **Rust File Service** (8200)  
4. **File Upload Service** (9016)  
5. **API Gateway** (8080)  
6. Other services as needed  

Docker Compose infra (MySQL, Redis, **file-service**, …) can be started without Java services; align Eureka flags with what you run.

## 7. Verification checklist

### Rust service

- Build/run: `cd ldms-file-service && cargo run` (or use Docker).
- **Health** (token required):

  ```bash
  curl -s -H "X-Internal-Token: dev-token-change-me" http://localhost:8200/health
  ```

  Expect JSON including **`"status":"UP"`** (and service metadata).

- With **`EUREKA_ENABLED=true`**, confirm registration on the Eureka dashboard (`http://localhost:8761`).

### Java file upload service

- Start with Config Server + Rust FS available (or local `application.yml` overrides).
- Confirm **`RustFsClient`** / **`rustfs`** settings in logs and no startup failures.

### Upload / retrieval / delete

Use the **Java** file-upload API paths your deployment exposes (e.g. gateway-prefixed **`/api/v1/frontend/...`** or direct **`/upload`** / **`/update`** for Feign — see service and gateway config). Verify:

1. Bytes stored under Rust layout **`{orgId}/{refType}/{uuid}.{ext}`** (and sidecar metadata if implemented).
2. MySQL **`file_upload`**: **`storage_provider = 'RUST_FS'`**, **`stored_file_name`** equals the Rust **file key**.
3. Retrieval responses include **`fileUploadDto.fileContent`** (base64) matching the original file.
4. **LOCAL** legacy rows still load from disk via **`readFileBytes()`** branching.
5. After delete: MySQL **`entity_status = 'DELETED'`**; Rust soft-delete behavior (e.g. marker) per Rust implementation; unauthenticated or deleted keys should not serve content.

## Port map (excerpt)

| Service | Port | Role |
|---------|------|------|
| Config Server | 8888 | Central config |
| Eureka | 8761 | Discovery |
| API Gateway | 8080 | Client entry |
| File Upload (Java) | 9016 | MySQL metadata + base64 APIs |
| **File Service (Rust)** | **8200** | **Byte storage** |
| IoT MQTT bridge (if used) | 8201 | Telemetry (do not confuse with 8200) |
