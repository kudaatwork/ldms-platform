import { NgModule } from '@angular/core';
import { OrganizationsRoutingModule } from './organizations-routing.module';
import { OrganizationsListComponent } from './pages/organizations-list/organizations-list.component';
import { OrganizationsByClassificationComponent } from './pages/organizations-by-classification/organizations-by-classification.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [OrganizationsListComponent, OrganizationsByClassificationComponent],
  imports: [SharedModule, OrganizationsRoutingModule],
})
export class OrganizationsModule {}
