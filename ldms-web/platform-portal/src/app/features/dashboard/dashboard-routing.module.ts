import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ClassificationGuard } from '../../core/guards/classification.guard';
import { DashboardComponent } from './pages/dashboard/dashboard.component';

const routes: Routes = [{ path: '', component: DashboardComponent, canActivate: [ClassificationGuard] }];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class DashboardRoutingModule {}
