import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DepartmentsWorkspaceComponent } from './pages/departments-workspace/departments-workspace.component';

const routes: Routes = [
  {
    path: '',
    component: DepartmentsWorkspaceComponent,
    data: { title: 'Departments', breadcrumb: 'Departments' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class DepartmentsRoutingModule {}
