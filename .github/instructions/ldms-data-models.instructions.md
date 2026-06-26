---
description: "LDMS entity, DTO, and model conventions across backend, Angular, and Flutter — how to define data models consistently"
applyTo: "ldms-backend/ldms-*/**/*.java,ldms-web/**/*.ts,ldms-mobile/**/*.dart"
---

# LDMS Data Models & Entities

Canonical patterns for JPA entities, DTOs, requests/responses, Angular interfaces, and Flutter models.

## Backend Entities (JPA)

### Audit Fields (Mandatory on Every Entity)

All entities MUST have these five fields. They are typically **inlined directly** (most services do **not** extend `BaseEntity`):

```java
@Enumerated(EnumType.STRING)
@Column(name = "entity_status", nullable = false, length = 50)
private EntityStatus entityStatus = EntityStatus.ACTIVE;

@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;

@Column(name = "created_by", nullable = false, length = 150)
private String createdBy;

@Column(name = "modified_at")
private LocalDateTime modifiedAt;

@Column(name = "modified_by", length = 150)
private String modifiedBy;
```

**Lifecycle callbacks:**
```java
@PrePersist
public void prePersist() {
    this.createdAt = LocalDateTime.now();
    this.entityStatus = EntityStatus.ACTIVE;
}

@PreUpdate
public void preUpdate() {
    this.modifiedAt = LocalDateTime.now();
}
```

> **Known inconsistency:** `ldms-user-management` uses `updatedAt` instead of `modifiedAt` in some entities. New code MUST use `modifiedAt`/`modifiedBy`.

### Entity Pattern

```java
@Entity
@Table(name = "fleet_asset")
@Getter @Setter @ToString
public class FleetAsset implements DomainMarkerInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Business fields
    @Column(name = "registration_number", nullable = false, length = 50)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 50)
    private FleetAssetType assetType;

    // Money / quantities
    @Column(name = "purchase_price", precision = 19, scale = 4)
    private BigDecimal purchasePrice;

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
}
```

### Rules

| Rule | Value |
|------|-------|
| IDs | `BIGINT AUTO_INCREMENT` · `Long` · `@GeneratedValue(IDENTITY)` |
| Enums | `@Enumerated(EnumType.STRING)` + `@Column(length = 50)` — never MySQL `ENUM` type |
| Money | `DECIMAL(19,4)` · `BigDecimal` with `precision = 19, scale = 4` |
| Quantities | `DECIMAL(19,2)` · `BigDecimal` with `precision = 19, scale = 2` |
| Timestamps | `DATETIME(6)` · `LocalDateTime` |
| Strings | Use `@Column(length = N)` with explicit limits |
| Soft delete | `EntityStatus.DELETED` — never physical SQL `DELETE` |
| Collections | `@ToString.Exclude` on `@OneToMany` to avoid circular refs |
| Marker interface | Each service defines an empty `DomainMarkerInterface` in `model` package |

### EntityStatus

Use the **shared-library** enum when possible (`projectlx.co.zw.shared_library.utils.enums.EntityStatus`):
- `ACTIVE`
- `INACTIVE`
- `DELETED`
- `ARCHIVED`

Some services define their own `EntityStatus` with extra values (e.g. `SUSPENDED` in user-management). Prefer the shared version for new entities.

## Backend DTOs / Requests / Responses

### Package Location

```
utils/
├── dtos/        # Flat data objects for cross-service/API use
├── requests/    # POST/PUT request bodies
├── responses/   # API response wrappers
└── enums/       # Service-specific enums
```

### DTO Pattern

```java
@Getter @Setter @ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FleetAssetDto {
    private Long id;
    private String registrationNumber;
    private String assetType;  // enum as string
    private BigDecimal purchasePrice;
    private String entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
}
```

### Request Pattern

```java
@Getter @Setter @ToString
public class CreateFleetAssetRequest {

    @NotBlank(message = "Registration number is required")
    @Size(max = 50, message = "Registration number must not exceed 50 characters")
    private String registrationNumber;

    @NotNull(message = "Asset type is required")
    private FleetAssetType assetType;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @Digits(integer = 15, fraction = 4, message = "Invalid price format")
    private BigDecimal purchasePrice;
}
```

### Response Pattern

All service-specific responses extend the shared `CommonResponse`:

```java
@Getter @Setter
public class FleetAssetResponse extends CommonResponse {
    private FleetAssetDto data;
}
```

## Angular Models (TypeScript)

### Pattern: Pure Interfaces + String Unions

No classes, no decorators, no code generation. Hand-written TypeScript `interface` and `type` aliases.

### Entity Interface

```typescript
export interface FleetAsset {
  id: number;
  registrationNumber: string;
  assetType: FleetAssetType;
  purchasePrice: number;
  entityStatus: EntityStatus;
  createdAt: string;      // ISO 8601 from backend LocalDateTime
  createdBy: string;
  modifiedAt?: string;
  modifiedBy?: string;
}
```

### Enum as String Union

Backend `@Enumerated(EnumType.STRING)` values map directly:

```typescript
export type FleetAssetType = 'RIG' | 'VAN' | 'TANKER' | 'FLATBED';

export type EntityStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED' | 'ARCHIVED';
```

### API Envelope

```typescript
export interface CommonApiFields {
  statusCode: number;
  isSuccess: boolean;
  message?: string;
  errorMessages?: string[];
}

export interface ApiResponse<T> extends CommonApiFields {
  data: T;
}
```

### Payload Interfaces

Mirror backend `*Request` classes exactly:

```typescript
export interface CreateFleetAssetPayload {
  registrationNumber: string;
  assetType: FleetAssetType;
  purchasePrice?: number;
}
```

### Presentation Row Interfaces

For tables, extend raw DTOs with computed display fields:

```typescript
export interface FleetAssetListRow extends FleetAsset {
  statusLabel: string;
  statusCss: string;
  createdLabel: string;
}
```

## Flutter Models (Dart)

> **Status:** The `ldms-mobile/` directories are currently placeholders (`.gitkeep` only). No Flutter implementation exists yet.

When implemented, follow this pattern:

```dart
class FleetAsset {
  final int id;
  final String registrationNumber;
  final String assetType;  // enum as string
  final double? purchasePrice;
  final String entityStatus;
  final DateTime createdAt;
  final String createdBy;

  FleetAsset({
    required this.id,
    required this.registrationNumber,
    required this.assetType,
    this.purchasePrice,
    required this.entityStatus,
    required this.createdAt,
    required this.createdBy,
  });

  factory FleetAsset.fromJson(Map<String, dynamic> json) => FleetAsset(
        id: json['id'] as int,
        registrationNumber: json['registrationNumber'] as String,
        assetType: json['assetType'] as String,
        purchasePrice: (json['purchasePrice'] as num?)?.toDouble(),
        entityStatus: json['entityStatus'] as String,
        createdAt: DateTime.parse(json['createdAt'] as String),
        createdBy: json['createdBy'] as String,
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'registrationNumber': registrationNumber,
        'assetType': assetType,
        'purchasePrice': purchasePrice,
        'entityStatus': entityStatus,
        'createdAt': createdAt.toIso8601String(),
        'createdBy': createdBy,
      };
}
```

## Cross-Layer Mapping

| Concept | Backend (Java) | Angular (TS) | Flutter (Dart) |
|---------|---------------|--------------|----------------|
| Entity ID | `Long` | `number` | `int` |
| Enum | `enum` + `@Enumerated(STRING)` | `type` string union | `String` |
| Money | `BigDecimal` | `number` | `double` |
| Date/Time | `LocalDateTime` | `string` (ISO) | `DateTime` |
| Status | `EntityStatus` | `'ACTIVE' \| 'INACTIVE' \| 'DELETED'` | `String` |
| API envelope | `CommonResponse` | `CommonApiFields` | `ApiResponse` class |
| Pagination | Spring `Page<T>` | `extractPagedResult()` util | `PaginationModel` |

## Shared Library (`ldms-shared-library`)

Reusable Java types for cross-service use:

| Category | Key Classes |
|----------|-------------|
| Base entity | `BaseEntity` (`@MappedSuperclass` — rarely extended) |
| Response | `CommonResponse`, `OrganizationResponse`, `PaginatedResponse` |
| DTOs | `OrganizationDto`, `UserDto`, `BranchDto`, `AddressDto` |
| Enums | `EntityStatus`, `Gender`, `TradingPartnerRole`, `InventoryDataSource` |

## Best Examples in Codebase

| Pattern | Best Example File |
|---------|-------------------|
| Canonical entity | `ldms-fleet-management/model/FleetAsset.java` |
| Entity with relationships | `ldms-inventory-management/model/Product.java` |
| Well-organized entity | `ldms-shipment-management/model/Shipment.java` |
| Angular interface | `ldms-web/*/src/app/features/*/models/*.model.ts` |
