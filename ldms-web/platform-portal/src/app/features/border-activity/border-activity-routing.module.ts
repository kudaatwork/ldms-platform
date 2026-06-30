import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BorderActivityWorkspaceComponent } from './pages/border-activity-workspace/border-activity-workspace.component';

const routes: Routes = [
  {
    path: '',
    component: BorderActivityWorkspaceComponent,
    data: { title: 'Border Activity', breadcrumb: 'Border Activity' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BorderActivityRoutingModule {}
