import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { OnboardingRoutingModule } from './onboarding-routing.module';
import { OnboardingStatusComponent } from './pages/onboarding-status/onboarding-status.component';

@NgModule({
  declarations: [OnboardingStatusComponent],
  imports: [SharedModule, OnboardingRoutingModule],
})
export class OnboardingModule {}
