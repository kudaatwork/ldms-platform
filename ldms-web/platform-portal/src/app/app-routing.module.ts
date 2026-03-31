import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ClassificationGuard } from './core/guards/classification.guard';
import { ShellLayoutComponent } from './layout/shell-layout/shell-layout.component';
import { PlaceholderPageComponent } from './features/portal/pages/placeholder-page/placeholder-page.component';

const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.module').then((m) => m.AuthModule),
  },
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
      },
      { path: 'products-inventory', component: PlaceholderPageComponent, data: { title: 'Products / Inventory' } },
      { path: 'purchase-orders', component: PlaceholderPageComponent, data: { title: 'Purchase Orders' } },
      { path: 'shipments', component: PlaceholderPageComponent, data: { title: 'Shipments' } },
      { path: 'fleet', component: PlaceholderPageComponent, data: { title: 'Fleet' } },
      { path: 'customers', component: PlaceholderPageComponent, data: { title: 'Customers' } },
      { path: 'documents', component: PlaceholderPageComponent, data: { title: 'Documents' } },
      { path: 'billing', component: PlaceholderPageComponent, data: { title: 'Billing' } },
      { path: 'reports', component: PlaceholderPageComponent, data: { title: 'Reports' } },
      { path: 'my-orders', component: PlaceholderPageComponent, data: { title: 'My Orders' } },
      { path: 'track-shipments', component: PlaceholderPageComponent, data: { title: 'Track Shipments' } },
      { path: 'deliveries', component: PlaceholderPageComponent, data: { title: 'Deliveries' } },
      { path: 'invoices', component: PlaceholderPageComponent, data: { title: 'Invoices' } },
      { path: 'drivers', component: PlaceholderPageComponent, data: { title: 'Drivers' } },
      { path: 'trips', component: PlaceholderPageComponent, data: { title: 'Trips' } },
      { path: 'active-clearances', component: PlaceholderPageComponent, data: { title: 'Active Clearances' } },
      { path: 'truck-visits', component: PlaceholderPageComponent, data: { title: 'Truck Visits' } },
      { path: 'fuel-log', component: PlaceholderPageComponent, data: { title: 'Fuel Log' } },
      { path: 'incidents', component: PlaceholderPageComponent, data: { title: 'Incidents' } },
      { path: 'service-log', component: PlaceholderPageComponent, data: { title: 'Service Log' } },
      { path: 'border-activity', component: PlaceholderPageComponent, data: { title: 'Border Activity' } },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
