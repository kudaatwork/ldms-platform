import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UiKitComponent } from './pages/ui-kit/ui-kit.component';

const routes: Routes = [
  {
    path: '',
    component: UiKitComponent,
    data: { title: 'UI Kit', breadcrumb: 'Components' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class UiKitRoutingModule {}

