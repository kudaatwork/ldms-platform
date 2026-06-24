---
description: Scaffold a new LDMS backend microservice following all Project LX conventions
applyTo: ldms-backend/ldms-*/**
---

# Skill: Create a New LDMS Microservice

Use this skill when the user wants to create a new microservice in the `ldms-backend/` directory.

## Prerequisites Check

Before starting, verify:
1. The service name matches one of the 23 documented services (see `docs/LDMS-SYSTEM-ARCHITECTURE.md`).
2. The module is currently **commented out** in `ldms-backend/pom.xml`.
3. No directory already exists with the target name.

## Step-by-Step Scaffolding

### 1. Create Directory and `pom.xml`

Create `ldms-backend/ldms-{service-name}/pom.xml` with this template:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>projectlx.co.zw</groupId>
        <artifactId>ldms-backend</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>ldms-{service-name}</artifactId>
    <packaging>jar</packaging>
    <name>LDMS {Service Name}</name>
    <description>{One-line description}</description>

    <properties>
        <projectlx.co.zw.version>1.0.0-SNAPSHOT</projectlx.co.zw.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>projectlx.co.zw</groupId>
            <artifactId>ldms-shared-library</artifactId>
            <version>${projectlx.co.zw.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2. Create Application Class

Path: `src/main/java/projectlx/{domain}/{subdomain}/{ServiceName}Application.java`

```java
package projectlx.{domain}.{subdomain};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
    "projectlx.{domain}.{subdomain}",
    "projectlx.co.zw.shared_library"
})
@EnableDiscoveryClient
public class {ServiceName}Application {
    public static void main(String[] args) {
        SpringApplication.run({ServiceName}Application.class, args);
    }
}
```

### 3. Create Mandatory Package Structure

Create all directories under `src/main/java/projectlx/{domain}/{subdomain}/`:

```
├── business/
│   ├── auditable/api/
│   ├── auditable/impl/
│   ├── config/
│   ├── logic/api/
│   ├── logic/impl/
│   ├── logic/support/
│   └── validator/api/
│   └── validator/impl/
├── clients/
├── config/
├── model/
├── repository/
│   ├── config/
│   └── specification/
├── service/
│   ├── config/
│   ├── processor/api/
│   ├── processor/impl/
│   └── rest/
│       ├── frontend/
│       ├── system/
│       └── backoffice/
├── tasks/
│   ├── api/
│   └── impl/
└── utils/
    ├── audit/
    ├── config/
    ├── constants/
    ├── dtos/
    ├── enums/
    ├── requests/
    ├── responses/
    ├── security/
    └── support/
```

Also create `src/main/resources/db/migration/` for Flyway.

### 4. Create `application.yml`

Path: `src/main/resources/application.yml`

```yaml
server:
  port: {port}

spring:
  application:
    name: ldms-{service-name}
  datasource:
    url: jdbc:mysql://localhost:3306/ldms_{service_name}?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

> Pick a unique port. Check existing services: user-management (8086), authentication (8083), api-gateway (8091), etc.

### 5. Create Bootstrap Migration

Path: `src/main/resources/db/migration/V1__create_base_tables.sql`

```sql
-- ================================================================
-- Migration: Create base tables
-- Version: V1
-- Purpose: Initial schema for {service-name}
-- ================================================================

-- Example entity table (replace with actual entities)
CREATE TABLE example_entity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    modified_at DATETIME(6),
    modified_by VARCHAR(100),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_example_entity_status ON example_entity(entity_status);
```

### 6. Uncomment Module in Parent POM

In `ldms-backend/pom.xml`, uncomment the matching `<module>` line:

```xml
<!-- Before -->
<!-- <module>ldms-{service-name}</module> -->

<!-- After -->
<module>ldms-{service-name}</module>
```

### 7. Verify Build

```bash
mvn -f ldms-backend/pom.xml clean package -DskipTests
```

## Rules

- Base package MUST be `projectlx.{domain}.{subdomain}` — never `projectlx.co.zw.{concatenated}`.
- `scanBasePackages` MUST include own base package + `projectlx.co.zw.shared_library`.
- All entities MUST have: `id`, `entity_status`, `created_at`, `created_by`, `modified_at`, `modified_by`.
- DB enums MUST be `VARCHAR(50)` — never MySQL ENUM.
- Money fields: `DECIMAL(19,4)`. Quantities: `DECIMAL(19,2)`. Timestamps: `DATETIME(6)`.
- Flyway files: `V{n}__{description}.sql` — sequential, immutable.
- Do NOT add services outside the 23-service list without architecture approval.
