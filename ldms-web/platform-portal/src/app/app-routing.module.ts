import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { ClassificationGuard } from './core/guards/classification.guard';
import { RoleGuard } from './core/guards/role.guard';
import { ShellLayoutComponent } from './layout/shell-layout/shell-layout.component';
import { PlaceholderPageComponent } from './features/portal/pages/placeholder-page/placeholder-page.component';
import { LandingComponent } from './features/landing/pages/landing/landing.component';
import { ContactDemoComponent } from './features/contact/pages/contact-demo/contact-demo.component';

const routes: Routes = [
  { path: 'welcome', component: LandingComponent },
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
        component: PlaceholderPageComponent,
        data: { title: 'Products / Inventory', breadcrumb: 'Products / Inventory' },
      },
      {
        path: 'purchase-orders',
        component: PlaceholderPageComponent,
        data: { title: 'Purchase Orders', breadcrumb: 'Purchase Orders' },
      },
      { path: 'shipments', component: PlaceholderPageComponent, data: { title: 'Shipments', breadcrumb: 'Shipments' } },
      {
        path: 'fleet',
        loadChildren: () => import('./features/fleet/fleet.module').then((m) => m.FleetModule),
        data: { breadcrumb: 'Fleet & Transporters' },
      },
      {
        path: 'customers',
        loadChildren: () => import('./features/customers/customers.module').then((m) => m.CustomersModule),
        data: { breadcrumb: 'Customers' },
      },
      {
        path: 'documents',
        loadChildren: () => import('./features/documents/documents.module').then((m) => m.DocumentsModule),
        data: { breadcrumb: 'Documents' },
      },
      { path: 'billing', component: PlaceholderPageComponent, data: { title: 'Billing', breadcrumb: 'Billing' } },
      { path: 'reports', component: PlaceholderPageComponent, data: { title: 'Reports', breadcrumb: 'Reports' } },
      { path: 'my-orders', component: PlaceholderPageComponent, data: { title: 'My Orders', breadcrumb: 'My Orders' } },
      {
        path: 'track-shipments',
        component: PlaceholderPageComponent,
        data: { title: 'Track Shipments', breadcrumb: 'Track Shipments' },
      },
      { path: 'deliveries', component: PlaceholderPageComponent, data: { title: 'Deliveries', breadcrumb: 'Deliveries' } },
      { path: 'invoices', component: PlaceholderPageComponent, data: { title: 'Invoices', breadcrumb: 'Invoices' } },
      { path: 'drivers', redirectTo: 'fleet/drivers', pathMatch: 'full' },
      { path: 'trips', component: PlaceholderPageComponent, data: { title: 'Trips', breadcrumb: 'Trips' } },
      {
        path: 'active-clearances',
        component: PlaceholderPageComponent,
        data: { title: 'Active Clearances', breadcrumb: 'Active Clearances' },
      },
      { path: 'truck-visits', component: PlaceholderPageComponent, data: { title: 'Truck Visits', breadcrumb: 'Truck Visits' } },
      { path: 'fuel-log', component: PlaceholderPageComponent, data: { title: 'Fuel Log', breadcrumb: 'Fuel Log' } },
      { path: 'incidents', component: PlaceholderPageComponent, data: { title: 'Incidents', breadcrumb: 'Incidents' } },
      { path: 'service-log', component: PlaceholderPageComponent, data: { title: 'Service Log', breadcrumb: 'Service Log' } },
      {
        path: 'border-activity',
        component: PlaceholderPageComponent,
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
