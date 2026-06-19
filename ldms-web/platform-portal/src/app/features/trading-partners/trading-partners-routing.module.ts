import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { TradingPartnersWorkspaceComponent } from './pages/trading-partners-workspace/trading-partners-workspace.component';

const routes: Routes = [
  {
    path: '',
    component: TradingPartnersWorkspaceComponent,
    data: { title: 'Trading Partners', breadcrumb: 'Trading Partners' },
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class TradingPartnersRoutingModule {}
