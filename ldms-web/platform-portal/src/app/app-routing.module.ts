import { inject, NgModule } from '@angular/core';
import { CanActivateFn, Router, RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { ClassificationGuard } from './core/guards/classification.guard';
import { RoleGuard } from './core/guards/role.guard';
import { SupplierClassificationGuard } from './core/guards/supplier-classification.guard';
import { WalletAccessGuard } from './core/guards/wallet-access.guard';
import { ShellLayoutComponent } from './layout/shell-layout/shell-layout.component';
import { PlaceholderPageComponent } from './features/portal/pages/placeholder-page/placeholder-page.component';
import { LandingComponent } from './features/landing/pages/landing/landing.component';
import { PricingPageComponent } from './features/landing/pages/pricing/pricing-page.component';
import { AboutPageComponent } from './features/landing/pages/about/about-page.component';
import { DemoPageComponent } from './features/landing/pages/demo/demo-page.component';
import { SupportPageComponent } from './features/landing/pages/support/support-page.component';
import { ContactDemoComponent } from './features/contact/pages/contact-demo/contact-demo.component';

const redirectBillingToSettings: CanActivateFn = () => {
  void inject(Router).navigate(['/settings'], { queryParams: { section: 'billing' } });
  return false;
};

const routes: Routes = [
  { path: 'welcome', component: LandingComponent },
  { path: 'pricing', component: PricingPageComponent },
  { path: 'about', component: AboutPageComponent },
  { path: 'demo', component: DemoPageComponent },
  { path: 'support', component: SupportPageComponent },
  { path: 'contact', component: ContactDemoComponent },
  {
    path: 'signup',
    loadChildren: () => import('./features/signup/signup.module').then((m) => m.SignupModule),
  },
  {
    path: 'onboarding',
    loadChildren: () => import('./features/onboarding/onboarding.module').then((m) => m.OnboardingModule),
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.module').then((m) => m.AuthModule),
  },
  {
    path: 'driver',
    loadChildren: () =>
      import('./features/driver-portal/driver-portal.module').then((m) => m.DriverPortalModule),
  },
  {
    path: 'clerk',
    loadChildren: () =>
      import('./features/clerk-portal/clerk-portal.module').then((m) => m.ClerkPortalModule),
  },
  { path: '', pathMatch: 'full', redirectTo: 'welcome' },
  {
    path: '',
    canActivate: [AuthGuard, ClassificationGuard, RoleGuard],
    component: ShellLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.module').then((m) => m.DashboardModule),
        data: { breadcrumb: 'Dashboard' },
      },
      {
        path: 'products-inventory',
        canActivate: [SupplierClassificationGuard, WalletAccessGuard],
        loadChildren: () =>
          import('./features/inventory/inventory.module').then((m) => m.InventoryModule),
        data: { breadcrumb: 'Inventory management' },
      },
      {
        path: 'purchase-orders',
        redirectTo: 'products-inventory/purchase-orders',
        pathMatch: 'full',
      },
      {
        path: 'shipments',
        canActivate: [WalletAccessGuard],
        loadChildren: () =>
          import('./features/trip-tracking/trip-tracking.module').then((m) => m.TripTrackingModule),
        data: { breadcrumb: 'Shipment management' },
      },
      {
        path: 'fleet',
        canActivate: [WalletAccessGuard],
        loadChildren: () => import('./features/fleet/fleet.module').then((m) => m.FleetModule),
        data: { breadcrumb: 'Fleet & Transporters' },
      },
      {
        path: 'customers',
        loadChildren: () => import('./features/customers/customers.module').then((m) => m.CustomersModule),
        data: { breadcrumb: 'Customers' },
      },
      {
        path: 'organization',
        loadChildren: () =>
          import('./features/organization-management/organization-management.module').then(
            (m) => m.OrganizationManagementModule,
          ),
        data: { breadcrumb: 'Organization management' },
      },
      {
        path: 'documents',
        loadChildren: () => import('./features/documents/documents.module').then((m) => m.DocumentsModule),
        data: { breadcrumb: 'Documents' },
      },
      { path: 'billing', canActivate: [redirectBillingToSettings], children: [] },
      {
        path: 'analytics',
        loadChildren: () => import('./features/reports/reports.module').then((m) => m.ReportsModule),
        data: { breadcrumb: 'Analytics' },
      },
      { path: 'reports', redirectTo: 'analytics/platform-usage', pathMatch: 'full' },
      { path: 'reports/usage-charges', redirectTo: 'analytics/platform-usage', pathMatch: 'full' },
      { path: 'reports/trip-journeys', redirectTo: 'analytics/trips', pathMatch: 'full' },
      { path: 'my-orders/departments', redirectTo: 'departments', pathMatch: 'full' },
      {
        path: 'departments',
        canActivate: [WalletAccessGuard],
        loadChildren: () =>
          import('./features/departments/departments.module').then((m) => m.DepartmentsModule),
        data: { breadcrumb: 'Departments' },
      },
      {
        path: 'my-orders',
        canActivate: [WalletAccessGuard],
        loadChildren: () =>
          import('./features/inventory/orders.module').then((m) => m.OrdersModule),
        data: { breadcrumb: 'Inventory management' },
      },
      {
        path: 'track-shipments',
        redirectTo: 'shipments/shipments',
        pathMatch: 'full',
      },
      { path: 'deliveries', redirectTo: 'my-orders/deliveries', pathMatch: 'full' },
      { path: 'invoices', component: PlaceholderPageComponent, data: { title: 'Invoices', breadcrumb: 'Invoices' } },
      { path: 'drivers', redirectTo: 'fleet/drivers', pathMatch: 'full' },
      {
        path: 'trips',
        redirectTo: 'shipments/trips',
        pathMatch: 'full',
      },
      {
        path: 'active-clearances',
        redirectTo: 'shipments/clearances',
        pathMatch: 'full',
      },
      {
        path: 'roadside',
        canActivate: [WalletAccessGuard],
        loadChildren: () =>
          import('./features/roadside-support/roadside-support.module').then((m) => m.RoadsideSupportModule),
        data: { breadcrumb: 'Roadside support' },
      },
      { path: 'truck-visits', redirectTo: 'roadside/truck-visits', pathMatch: 'full' },
      { path: 'fuel-log', redirectTo: 'roadside/fuel-log', pathMatch: 'full' },
      { path: 'incidents', redirectTo: 'roadside/incidents', pathMatch: 'full' },
      { path: 'service-log', redirectTo: 'roadside/service-log', pathMatch: 'full' },
      {
        path: 'border-activity',
        loadChildren: () =>
          import('./features/border-activity/border-activity.module').then((m) => m.BorderActivityModule),
        data: { title: 'Border Activity', breadcrumb: 'Border Activity' },
      },
      {
        path: 'users',
        loadChildren: () => import('./features/users/users.module').then((m) => m.UsersModule),
        data: { breadcrumb: 'Users' },
      },
      {
        path: 'activity',
        loadChildren: () => import('./features/activity/activity.module').then((m) => m.ActivityModule),
        data: { breadcrumb: 'Audit Log' },
      },
      {
        path: 'account',
        loadChildren: () => import('./features/account/account.module').then((m) => m.AccountModule),
        data: { breadcrumb: 'My account' },
      },
      {
        path: 'settings',
        loadChildren: () => import('./features/settings/settings.module').then((m) => m.SettingsModule),
        data: { breadcrumb: 'Settings' },
      },
      {
        path: 'trading-partners',
        loadChildren: () =>
          import('./features/trading-partners/trading-partners.module').then((m) => m.TradingPartnersModule),
        data: { breadcrumb: 'Trading Partners' },
      },
      {
        path: 'help',
        loadChildren: () =>
          import('./features/help-support/help-support.module').then((m) => m.HelpSupportModule),
        data: { breadcrumb: 'Help & Support' },
      },
    ],
  },
  { path: '**', redirectTo: 'welcome' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
