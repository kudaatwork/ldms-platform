import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SignupWizardComponent } from './pages/signup-wizard/signup-wizard.component';

const routes: Routes = [{ path: '', component: SignupWizardComponent }];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class SignupRoutingModule {}
