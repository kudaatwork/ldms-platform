---
description: "Scaffold a new JPA entity, DTO, request, response, and repository following LDMS canonical patterns"
applyTo: "ldms-backend/ldms-*/**/*.java"
---

# Skill: Create a New Entity in LDMS

Use this skill when adding a new domain entity to an LDMS microservice.

## Prerequisites Check

1. Identify the target microservice and its base package (e.g. `projectlx.fleet.management`).
2. Verify the entity name follows the domain language used in that service.
3. Check if a similar entity exists to copy patterns from.

## Step-by-Step

### 1. Create the Entity

Path: `src/main/java/projectlx/<domain>/<subdomain>/model/{Entity}.java`

```java
package projectlx.<domain>.<subdomain>.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "{table_name}")
@Getter
@Setter
@ToString
public class {Entity} implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === BUSINESS FIELDS ===
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private {Entity}Status status;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    // === FOREIGN KEYS ===
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    // === AUDIT FIELDS ===
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 150)
    private String modifiedBy;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.entityStatus = EntityStatus.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {
        this.modifiedAt = LocalDateTime.now();
    }
}
```

### 2. Create the Status Enum (if needed)

Path: `src/main/java/projectlx/<domain>/<subdomain>/utils/enums/{Entity}Status.java`

```java
package projectlx.<domain>.<subdomain>.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum {Entity}Status {
    DRAFT("DRAFT"),
    ACTIVE("ACTIVE"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String status;
}
```

### 3. Create the DTO

Path: `src/main/java/projectlx/<domain>/<subdomain>/utils/dtos/{Entity}Dto.java`

```java
package projectlx.<domain>.<subdomain>.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class {Entity}Dto {
    private Long id;
    private String name;
    private String status;
    private BigDecimal amount;
    private Long organizationId;
    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
```

### 4. Create Request Objects

Path: `src/main/java/projectlx/<domain>/<subdomain>/utils/requests/Create{Entity}Request.java`

```java
package projectlx.<domain>.<subdomain>.utils.requests;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class Create{Entity}Request {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotNull(message = "Organization ID is required")
    private Long organizationId;

    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be positive")
    @Digits(integer = 15, fraction = 4, message = "Invalid amount format")
    private BigDecimal amount;
}
```

Path: `src/main/java/projectlx/<domain>/<subdomain>/utils/requests/Edit{Entity}Request.java`

```java
package projectlx.<domain>.<subdomain>.utils.requests;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class Edit{Entity}Request {

    @NotNull(message = "ID is required")
    private Long id;

    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @DecimalMin(value = "0.0", message = "Amount must be non-negative")
    @Digits(integer = 15, fraction = 4, message = "Invalid amount format")
    private BigDecimal amount;
}
```

### 5. Create Response Object

Path: `src/main/java/projectlx/<domain>/<subdomain>/utils/responses/{Entity}Response.java`

```java
package projectlx.<domain>.<subdomain>.utils.responses;

import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.dtos.CommonResponse;
import projectlx.<domain>.<subdomain>.utils.dtos.{Entity}Dto;

@Getter
@Setter
public class {Entity}Response extends CommonResponse {
    private {Entity}Dto data;
}
```

### 6. Create Repository

Path: `src/main/java/projectlx/<domain>/<subdomain>/repository/{Entity}Repository.java`

```java
package projectlx.<domain>.<subdomain>.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import projectlx.<domain>.<subdomain>.model.{Entity};
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface {Entity}Repository extends JpaRepository<{Entity}, Long> {

    Optional<{Entity}> findByIdAndEntityStatus(Long id, EntityStatus entityStatus);

    List<{Entity}> findAllByOrganizationIdAndEntityStatus(Long organizationId, EntityStatus entityStatus);
}
```

### 7. Create Flyway Migration

Path: `src/main/resources/db/migration/V{n}__create_{table_name}_table.sql`

```sql
-- ================================================================
-- Migration: Create {Entity} table
-- Version: V{n}
-- Service: ldms-{service-name}
-- ================================================================

CREATE TABLE {table_name} (
    id BIGINT NOT NULL AUTO_INCREMENT,

    -- Business fields
    name VARCHAR(200) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount DECIMAL(19,4) DEFAULT 0.0000,

    -- Foreign keys
    organization_id BIGINT NOT NULL,

    -- Audit fields
    entity_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    created_by VARCHAR(150) NOT NULL,
    modified_at DATETIME(6),
    modified_by VARCHAR(150),

    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_{table}_org_status ON {table_name}(organization_id, entity_status);
CREATE INDEX idx_{table}_status ON {table_name}(status);
```

## Checklist

- [ ] Entity has all 5 audit fields (`entity_status`, `created_at`, `created_by`, `modified_at`, `modified_by`)
- [ ] Uses `@Enumerated(EnumType.STRING)` for all enum fields
- [ ] Uses `precision = 19, scale = 4` for money fields
- [ ] Uses `precision = 19, scale = 2` for quantity fields
- [ ] Has `@PrePersist` and `@PreUpdate` lifecycle callbacks
- [ ] DTO uses `@JsonInclude(JsonInclude.Include.NON_NULL)`
- [ ] Request has Jakarta validation annotations
- [ ] Response extends `CommonResponse`
- [ ] Repository extends `JpaRepository`
- [ ] Flyway migration uses `utf8mb4_unicode_ci`
