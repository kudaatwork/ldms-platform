import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { GuestGuard } from '../../core/guards/guest.guard';
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password.component';
import { LoginComponent } from './pages/login/login.component';
import { ResetPasswordComponent } from './pages/reset-password/reset-password.component';
import { SetupCredentialsComponent } from './pages/setup-credentials/setup-credentials.component';
import { SetupCredentialsGuard } from '../../core/guards/setup-credentials.guard';
import { VerifyEmailComponent } from './pages/verify-email/verify-email.component';
import { VerifyOrganizationEmailComponent } from './pages/verify-organization-email/verify-organization-email.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [GuestGuard] },
  { path: 'verify-email', component: VerifyEmailComponent },
  { path: 'verify-organization-email', component: VerifyOrganizationEmailComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  {
    path: 'setup-credentials',
    component: SetupCredentialsComponent,
    canActivate: [SetupCredentialsGuard],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AuthRoutingModule {}
