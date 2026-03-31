import { NgModule } from '@angular/core';
import { UsersRoutingModule } from './users-routing.module';
import { UsersListComponent } from './pages/users-list/users-list.component';
import { UsersRolesComponent } from './pages/users-roles/users-roles.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [UsersListComponent, UsersRolesComponent],
  imports: [SharedModule, UsersRoutingModule],
})
export class UsersModule {}
