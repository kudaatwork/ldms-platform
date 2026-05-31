import { NgModule } from '@angular/core';
import { OrganizationsRoutingModule } from './organizations-routing.module';
import { OrganizationsListComponent } from './pages/organizations-list/organizations-list.component';
import { OrganizationsByClassificationComponent } from './pages/organizations-by-classification/organizations-by-classification.component';
import { BranchesListComponent } from './pages/branches-list/branches-list.component';
import { AgentsListComponent } from './pages/agents-list/agents-list.component';
import { IndustriesListComponent } from './pages/industries-list/industries-list.component';
import { OrganizationDetailShellComponent } from './pages/organization-detail-shell/organization-detail-shell.component';
import { BranchFormDialogComponent } from './pages/branch-form-dialog/branch-form-dialog.component';
import { AgentFormDialogComponent } from './pages/agent-form-dialog/agent-form-dialog.component';
import { LinkOrganizationDialogComponent } from './pages/link-organization-dialog/link-organization-dialog.component';
import { SharedModule } from '../../shared/shared.module';
import { OrganizationDocumentsPanelComponent } from '../../shared/components/organization-documents-panel/organization-documents-panel.component';
import { OrganizationContactPersonPanelComponent } from '../../shared/components/organization-contact-person-panel/organization-contact-person-panel.component';

@NgModule({
  declarations: [
    OrganizationsListComponent,
    OrganizationsByClassificationComponent,
    BranchesListComponent,
    AgentsListComponent,
    IndustriesListComponent,
    OrganizationDetailShellComponent,
  ],
  imports: [
    SharedModule,
    OrganizationsRoutingModule,
    OrganizationDocumentsPanelComponent,
    OrganizationContactPersonPanelComponent,
    BranchFormDialogComponent,
    AgentFormDialogComponent,
    LinkOrganizationDialogComponent,
  ],
})
export class OrganizationsModule {}
