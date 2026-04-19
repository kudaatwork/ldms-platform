import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';

const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.module').then((m) => m.AuthModule),
  },
  {
    path: '',
    canActivate: [AuthGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.module').then((m) => m.DashboardModule),
      },
      {
        path: 'activity',
        loadChildren: () =>
          import('./features/activity/activity.module').then((m) => m.ActivityModule),
      },
      {
        path: 'kyc',
        loadChildren: () => import('./features/kyc/kyc.module').then((m) => m.KycModule),
        data: { breadcrumb: 'KYC' },
      },
      {
        path: 'organizations',
        loadChildren: () =>
          import('./features/organizations/organizations.module').then((m) => m.OrganizationsModule),
        data: { breadcrumb: 'Organizations' },
      },
      {
        path: 'users',
        loadChildren: () => import('./features/users/users.module').then((m) => m.UsersModule),
        data: { breadcrumb: 'Users' },
      },
      {
        path: 'locations',
        loadChildren: () =>
          import('./features/locations/locations.module').then((m) => m.LocationsModule),
        data: { breadcrumb: 'Locations' },
      },
      {
        path: 'notifications',
        loadChildren: () =>
          import('./features/notifications/notifications.module').then((m) => m.NotificationsModule),
        data: { breadcrumb: 'Notifications' },
      },
      {
        path: 'system',
        loadChildren: () => import('./features/system/system.module').then((m) => m.SystemModule),
        data: { breadcrumb: 'System' },
      },
      {
        path: 'ui-kit',
        loadChildren: () => import('./features/ui-kit/ui-kit.module').then((m) => m.UiKitModule),
        data: { breadcrumb: 'UI Kit' },
      },
    ],
  },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, {
      scrollPositionRestoration: 'enabled',
      anchorScrolling: 'enabled',
    }),
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {}
