import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password.component';
import { LoginComponent } from './pages/login/login.component';
import { ResetPasswordComponent } from './pages/reset-password/reset-password.component';
import { VerifyEmailComponent } from './pages/verify-email/verify-email.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent, data: { title: 'Sign in' } },
  { path: 'forgot-password', component: ForgotPasswordComponent, data: { title: 'Forgot password' } },
  { path: 'reset-password', component: ResetPasswordComponent, data: { title: 'Reset password' } },
  { path: 'verify-email', component: VerifyEmailComponent, data: { title: 'Verify email' } },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AuthRoutingModule {}
