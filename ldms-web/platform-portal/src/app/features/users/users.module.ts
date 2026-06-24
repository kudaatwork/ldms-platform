import { NgModule } from '@angular/core';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { UsersRoutingModule } from './users-routing.module';
import { UsersListComponent } from './pages/users-list/users-list.component';
import { UsersRolesComponent } from './pages/users-roles/users-roles.component';
import { UsersGroupsComponent } from './pages/users-groups/users-groups.component';
import { UserProfileShellComponent } from './pages/user-profile-shell/user-profile-shell.component';
import { UserTypesComponent } from './pages/user-types/user-types.component';
import { UsersHubNavComponent } from './components/users-hub-nav/users-hub-nav.component';
import { AssignRolesDialogComponent } from './components/assign-roles-dialog/assign-roles-dialog.component';
import { UserDocumentDetailDialogComponent } from './components/user-document-detail-dialog/user-document-detail-dialog.component';
import { UserAssignUserGroupDialogComponent } from './components/user-assign-user-group-dialog/user-assign-user-group-dialog.component';
import { UserGroupMembersDialogComponent } from './components/user-group-members-dialog/user-group-members-dialog.component';
import { UserAddressCascadeFieldsComponent } from './components/user-address-cascade-fields/user-address-cascade-fields.component';
import { SharedModule } from '../../shared/shared.module';
import { UsersDialogsModule } from './users-dialogs.module';

@NgModule({
  declarations: [
    UsersListComponent,
    UsersRolesComponent,
    UsersGroupsComponent,
    UserProfileShellComponent,
    UserTypesComponent,
    UsersHubNavComponent,
    AssignRolesDialogComponent,
  ],
  imports: [
    SharedModule,
    UsersDialogsModule,
    ScrollingModule,
    UsersRoutingModule,
    UserAssignUserGroupDialogComponent,
    UserGroupMembersDialogComponent,
    UserAddressCascadeFieldsComponent,
    UserDocumentDetailDialogComponent,
  ],
  exports: [UsersDialogsModule, UserDocumentDetailDialogComponent],
})
export class UsersModule {}
