import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { MyAccountComponent } from './pages/my-account/my-account.component';

const routes: Routes = [
  {
    path: '',
    component: MyAccountComponent,
    data: { title: 'My account', breadcrumb: 'My account' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AccountRoutingModule {}
