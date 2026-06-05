import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { AuthRoutingModule } from './auth-routing.module';
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password.component';
import { LoginComponent } from './pages/login/login.component';
import { ResetPasswordComponent } from './pages/reset-password/reset-password.component';
import { SetupCredentialsComponent } from './pages/setup-credentials/setup-credentials.component';
import { VerifyEmailComponent } from './pages/verify-email/verify-email.component';
import { VerifyOrganizationEmailComponent } from './pages/verify-organization-email/verify-organization-email.component';

@NgModule({
  declarations: [
    LoginComponent,
    ForgotPasswordComponent,
    ResetPasswordComponent,
    SetupCredentialsComponent,
    VerifyEmailComponent,
    VerifyOrganizationEmailComponent,
  ],
  imports: [SharedModule, AuthRoutingModule],
})
export class AuthModule {}
