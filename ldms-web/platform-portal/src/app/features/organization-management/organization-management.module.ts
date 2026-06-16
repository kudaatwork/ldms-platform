import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { FleetModule } from '../fleet/fleet.module';
import { OrgBranchesPageComponent } from './pages/org-branches-page/org-branches-page.component';
import { OrgAgentsPageComponent } from './pages/org-agents-page/org-agents-page.component';
import { OrgTransportersPageComponent } from './pages/org-transporters-page/org-transporters-page.component';
import { BranchFormDialogComponent } from './components/branch-form-dialog/branch-form-dialog.component';
import { LinkWarehouseDialogComponent } from './components/link-warehouse-dialog/link-warehouse-dialog.component';
import { AgentFormDialogComponent } from './components/agent-form-dialog/agent-form-dialog.component';
import { InventoryDialogsModule } from '../inventory/inventory-dialogs.module';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'branches' },
  {
    path: 'branches',
    component: OrgBranchesPageComponent,
    data: { title: 'Branches', breadcrumb: 'Branches', branchScope: 'top-level' },
  },
  {
    path: 'sub-branches',
    component: OrgBranchesPageComponent,
    data: { title: 'Sub-branches & depots', breadcrumb: 'Sub-branches', branchScope: 'sub-level' },
  },
  {
    path: 'agents',
    component: OrgAgentsPageComponent,
    data: { title: 'Agents', breadcrumb: 'Agents' },
  },
  {
    path: 'transporters',
    component: OrgTransportersPageComponent,
    data: { title: 'Transporters', breadcrumb: 'Transporters' },
  },
];

@NgModule({
  declarations: [
    OrgBranchesPageComponent,
    OrgAgentsPageComponent,
    OrgTransportersPageComponent,
    LinkWarehouseDialogComponent,
  ],
  imports: [SharedModule, FleetModule, InventoryDialogsModule, RouterModule.forChild(routes), BranchFormDialogComponent, AgentFormDialogComponent],
})
export class OrganizationManagementModule {}
