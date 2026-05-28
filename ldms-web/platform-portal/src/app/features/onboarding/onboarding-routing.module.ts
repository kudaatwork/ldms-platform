import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { OnboardingStatusComponent } from './pages/onboarding-status/onboarding-status.component';

const routes: Routes = [
  { path: 'status', component: OnboardingStatusComponent },
  { path: '', pathMatch: 'full', redirectTo: 'status' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class OnboardingRoutingModule {}
