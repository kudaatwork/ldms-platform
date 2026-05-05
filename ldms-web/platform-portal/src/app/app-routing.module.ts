import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ClassificationGuard } from './core/guards/classification.guard';
import { ShellLayoutComponent } from './layout/shell-layout/shell-layout.component';
import { PlaceholderPageComponent } from './features/portal/pages/placeholder-page/placeholder-page.component';
import { LandingComponent } from './features/landing/pages/landing/landing.component';

const routes: Routes = [
  { path: 'welcome', component: LandingComponent },
  {
    path: 'signup',
    loadChildren: () => import('./features/signup/signup.module').then((m) => m.SignupModule),
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.module').then((m) => m.AuthModule),
  },
  { path: '', pathMatch: 'full', redirectTo: 'welcome' },
  {
    path: '',
    canActivate: [ClassificationGuard],
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
      { path: 'fleet', component: PlaceholderPageComponent, data: { title: 'Fleet', breadcrumb: 'Fleet' } },
      { path: 'customers', component: PlaceholderPageComponent, data: { title: 'Customers', breadcrumb: 'Customers' } },
      { path: 'documents', component: PlaceholderPageComponent, data: { title: 'Documents', breadcrumb: 'Documents' } },
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
      { path: 'drivers', component: PlaceholderPageComponent, data: { title: 'Drivers', breadcrumb: 'Drivers' } },
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
        path: 'account',
        component: PlaceholderPageComponent,
        data: { title: 'My account', breadcrumb: 'My account' },
      },
      {
        path: 'settings',
        component: PlaceholderPageComponent,
        data: { title: 'Settings', breadcrumb: 'Settings' },
      },
      {
        path: 'help',
        component: PlaceholderPageComponent,
        data: { title: 'Help & Support', breadcrumb: 'Help & Support' },
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
