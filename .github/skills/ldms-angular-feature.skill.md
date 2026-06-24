---
description: Scaffold a new Angular feature module with components, services, and routing in LDMS web portals
applyTo: ldms-web/**/src/app/features/**/*.ts
---

# Skill: Create a New Angular Feature in LDMS

Use this skill when the user wants to add a new feature module to `ldms-web/admin-portal` or `ldms-web/platform-portal`.

## Prerequisites Check

1. Identify which portal: **admin-portal** (internal admin UI, port 4200) or **platform-portal** (customer-facing, port 4201).
2. Check if a similar feature exists to copy patterns from (e.g. `features/settings`, `features/billing`).
3. Ensure the feature is registered in `app-routing.module.ts` with lazy loading.

## Step-by-Step Scaffolding

### 1. Create Feature Directory Structure

```
src/app/features/{feature-name}/
├── components/
│   ├── {entity}-list/
│   │   ├── {entity}-list.component.ts
│   │   ├── {entity}-list.component.html
│   │   └── {entity}-list.component.scss
│   ├── {entity}-form/
│   │   ├── {entity}-form.component.ts
│   │   ├── {entity}-form.component.html
│   │   └── {entity}-form.component.scss
│   └── {entity}-detail/
│       ├── {entity}-detail.component.ts
│       ├── {entity}-detail.component.html
│       └── {entity}-detail.component.scss
├── services/
│   └── {entity}.service.ts
├── models/
│   └── {entity}.model.ts
├── {feature-name}.component.ts
├── {feature-name}.component.html
├── {feature-name}.module.ts
└── {feature-name}-routing.module.ts
```

### 2. Create Model

Path: `models/{entity}.model.ts`

```typescript
export interface {Entity} {
  id: number;
  name: string;
  // ... other fields
  entityStatus: string;
  createdAt: string;
  createdBy: string;
  modifiedAt?: string;
  modifiedBy?: string;
}
```

### 3. Create Service

Path: `services/{entity}.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { {Entity} } from '../models/{entity}.model';

@Injectable({ providedIn: 'root' })
export class {Entity}Service {
  private readonly apiUrl = '/ldms-{service-name}/v1/frontend/{entity}';

  constructor(private http: HttpClient) {}

  getAll(): Observable<{Entity}[]> {
    return this.http.get<{Entity}[]>(`${this.apiUrl}/all`);
  }

  getById(id: number): Observable<{Entity}> {
    return this.http.get<{Entity}>(`${this.apiUrl}/${id}`);
  }

  create(payload: Partial<{Entity}>): Observable<{Entity}> {
    return this.http.post<{Entity}>(`${this.apiUrl}/create`, payload);
  }

  update(id: number, payload: Partial<{Entity}>): Observable<{Entity}> {
    return this.http.put<{Entity}>(`${this.apiUrl}/update/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/delete/${id}`);
  }
}
```

### 4. Create List Component

Path: `components/{entity}-list/{entity}-list.component.ts`

```typescript
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { {Entity}Service } from '../../services/{entity}.service';
import { {Entity} } from '../../models/{entity}.model';
import { NotificationService } from '@core/services/notification.service';

@Component({
  selector: 'app-{entity}-list',
  templateUrl: './{entity}-list.component.html',
  styleUrls: ['./{entity}-list.component.scss']
})
export class {Entity}ListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  entities: {Entity}[] = [];
  loading = false;

  constructor(
    private entityService: {Entity}Service,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadEntities();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadEntities(): void {
    this.loading = true;
    this.entityService.getAll()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.entities = data;
          this.loading = false;
        },
        error: (err) => {
          this.notificationService.showError('Failed to load {entities}');
          this.loading = false;
        }
      });
  }
}
```

### 5. Create Feature Module

Path: `{feature-name}.module.ts`

```typescript
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AngularMaterialModule } from '@shared/angular-material.module';

import { {FeatureName}RoutingModule } from './{feature-name}-routing.module';
import { {FeatureName}Component } from './{feature-name}.component';
import { {Entity}ListComponent } from './components/{entity}-list/{entity}-list.component';
import { {Entity}FormComponent } from './components/{entity}-form/{entity}-form.component';

@NgModule({
  declarations: [
    {FeatureName}Component,
    {Entity}ListComponent,
    {Entity}FormComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    AngularMaterialModule,
    {FeatureName}RoutingModule
  ]
})
export class {FeatureName}Module {}
```

### 6. Create Routing Module

Path: `{feature-name}-routing.module.ts`

```typescript
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { {FeatureName}Component } from './{feature-name}.component';
import { {Entity}ListComponent } from './components/{entity}-list/{entity}-list.component';
import { {Entity}FormComponent } from './components/{entity}-form/{entity}-form.component';

const routes: Routes = [
  {
    path: '',
    component: {FeatureName}Component,
    children: [
      { path: '', component: {Entity}ListComponent },
      { path: 'create', component: {Entity}FormComponent },
      { path: 'edit/:id', component: {Entity}FormComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class {FeatureName}RoutingModule {}
```

### 7. Register in App Routing

Add lazy-loaded route to `app-routing.module.ts`:

```typescript
{
  path: '{feature-name}',
  loadChildren: () => import('./features/{feature-name}/{feature-name}.module')
    .then(m => m.{FeatureName}Module),
  canActivate: [AuthGuard]
}
```

### 8. Add Navigation Link

Add to the sidebar/navigation component (e.g. `shared/components/sidebar/sidebar.component.html`):

```html
<a mat-list-item routerLink="/{feature-name}" routerLinkActive="active">
  <mat-icon>{icon}</mat-icon>
  <span>{Feature Label}</span>
</a>
```

## Rules

- Use **NgModule-based** architecture (not standalone components).
- Import `AngularMaterialModule` from `@shared/angular-material.module` — do not import individual Material modules.
- Use `lx-*` design system classes for styling (`lx-btn-primary`, `lx-mat-table`, `lx-dialog-shell`, etc.).
- Services use `providedIn: 'root'`.
- Use `takeUntil(this.destroy$)` pattern for subscription cleanup.
- Use `NotificationService` from `@core/services/notification.service` for user feedback.
- API base URLs: `/ldms-{service}/v1/frontend/{entity}` for admin portal, `/api/v1/...` for platform portal.
- Do NOT introduce new npm dependencies (no Tailwind, no Bootstrap).
- Do NOT break dark-mode compatibility — test against `html.theme-dark`.
