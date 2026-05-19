import { NgModule } from '@angular/core';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { UsersRoutingModule } from './users-routing.module';
import { UsersListComponent } from './pages/users-list/users-list.component';
import { UsersRolesComponent } from './pages/users-roles/users-roles.component';
import { UsersGroupsComponent } from './pages/users-groups/users-groups.component';
import { UserProfileShellComponent } from './pages/user-profile-shell/user-profile-shell.component';
import { UserTypesComponent } from './pages/user-types/user-types.component';
import { AssignRolesDialogComponent } from './components/assign-roles-dialog/assign-roles-dialog.component';
import { UserEditProfileDialogComponent } from './components/user-edit-profile-dialog/user-edit-profile-dialog.component';
import { UserDocumentDetailDialogComponent } from './components/user-document-detail-dialog/user-document-detail-dialog.component';
import { UserEditAddressDialogComponent } from './components/user-edit-address-dialog/user-edit-address-dialog.component';
import { UserEditAccountDialogComponent } from './components/user-edit-account-dialog/user-edit-account-dialog.component';
import { UserEditSecurityDialogComponent } from './components/user-edit-security-dialog/user-edit-security-dialog.component';
import { UserAssignUserGroupDialogComponent } from './components/user-assign-user-group-dialog/user-assign-user-group-dialog.component';
import { UserGroupMembersDialogComponent } from './components/user-group-members-dialog/user-group-members-dialog.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [
    UsersListComponent,
    UsersRolesComponent,
    UsersGroupsComponent,
    UserProfileShellComponent,
    UserTypesComponent,
    AssignRolesDialogComponent,
    UserEditProfileDialogComponent,
    UserDocumentDetailDialogComponent,
    UserEditAddressDialogComponent,
    UserEditAccountDialogComponent,
    UserEditSecurityDialogComponent,
  ],
  imports: [
    SharedModule,
    ScrollingModule,
    UsersRoutingModule,
    UserAssignUserGroupDialogComponent,
    UserGroupMembersDialogComponent,
  ],
})
export class UsersModule {}
