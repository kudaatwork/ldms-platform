import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UsersListComponent } from './pages/users-list/users-list.component';
import { UsersRolesComponent } from './pages/users-roles/users-roles.component';
import { UsersGroupsComponent } from './pages/users-groups/users-groups.component';
import { UserProfileShellComponent } from './pages/user-profile-shell/user-profile-shell.component';
import { UserTypesComponent } from './pages/user-types/user-types.component';

const routes: Routes = [
  {
    path: '',
    component: UsersListComponent,
    data: { title: 'All Users', breadcrumb: 'All' },
  },
  {
    path: 'roles',
    component: UsersRolesComponent,
    data: { title: 'Roles', breadcrumb: 'Roles' },
  },
  {
    path: 'groups',
    component: UsersGroupsComponent,
    data: { title: 'User Groups', breadcrumb: 'Groups' },
  },
  {
    path: 'groups/:groupId/roles',
    component: UsersRolesComponent,
    data: { title: 'Group Roles', breadcrumb: 'Group Roles' },
  },
  {
    path: 'types',
    component: UserTypesComponent,
    data: { title: 'User Types', breadcrumb: 'User Types' },
  },
  {
    path: ':userId/profile',
    component: UserProfileShellComponent,
    data: { title: 'User Profile', breadcrumb: 'Profile', section: 'profile' },
  },
  {
    path: ':userId/account',
    component: UserProfileShellComponent,
    data: { title: 'User Account', breadcrumb: 'Account', section: 'account' },
  },
  {
    path: ':userId/preferences',
    component: UserProfileShellComponent,
    data: { title: 'User Preferences', breadcrumb: 'Preferences', section: 'preferences' },
  },
  {
    path: ':userId/security-policies',
    component: UserProfileShellComponent,
    data: { title: 'Security details', breadcrumb: 'Security details', section: 'security-policies' },
  },
  {
    path: ':userId/addresses',
    component: UserProfileShellComponent,
    data: { title: 'User Addresses', breadcrumb: 'Addresses', section: 'addresses' },
  },
  {
    path: ':userId/password',
    component: UserProfileShellComponent,
    data: { title: 'Change Password', breadcrumb: 'Password', section: 'password' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class UsersRoutingModule {}
