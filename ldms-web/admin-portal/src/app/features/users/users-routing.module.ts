import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UsersListComponent } from './pages/users-list/users-list.component';
import { UsersRolesComponent } from './pages/users-roles/users-roles.component';

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
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class UsersRoutingModule {}
