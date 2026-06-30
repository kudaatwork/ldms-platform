import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { DepartmentDialogComponent } from '../inventory/components/department-dialog/department-dialog.component';
import { DepartmentsRoutingModule } from './departments-routing.module';
import { DepartmentsWorkspaceComponent } from './pages/departments-workspace/departments-workspace.component';

/** Organisation departments for purchase requisitions (/departments). */
@NgModule({
  declarations: [DepartmentsWorkspaceComponent, DepartmentDialogComponent],
  imports: [SharedModule, DepartmentsRoutingModule],
})
export class DepartmentsModule {}
