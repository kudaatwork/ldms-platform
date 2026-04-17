# Actix/time/ICU need a recent toolchain; aws-sdk-s3 / aws-config (S3 feature) require rustc ≥1.91.1.
FROM rust:1.91-slim AS builder
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends pkg-config libssl-dev \
    && rm -rf /var/lib/apt/lists/*

COPY Cargo.toml Cargo.lock ./
COPY src ./src
COPY config.toml ./

RUN cargo build --release --features s3

FROM rust:1.91-slim AS runtime
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/release/ldms-file-service /app/ldms-file-service
COPY config.toml /app/config.toml

EXPOSE 8200

CMD ["./ldms-file-service"]
