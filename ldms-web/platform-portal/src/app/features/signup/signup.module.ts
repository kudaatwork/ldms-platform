import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { SignupRoutingModule } from './signup-routing.module';
import { SignupWizardComponent } from './pages/signup-wizard/signup-wizard.component';

@NgModule({
  declarations: [SignupWizardComponent],
  imports: [SharedModule, SignupRoutingModule],
})
export class SignupModule {}
