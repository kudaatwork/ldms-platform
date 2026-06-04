import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { StaticShellPageComponent } from './shared/static-shell-page/static-shell-page.component';
import { MyAccountComponent } from './features/account/pages/my-account/my-account.component';
import { CoreModule } from './core/core.module';
import { SharedModule } from './shared/shared.module';
import { UsersDialogsModule } from './features/users/users-dialogs.module';
import { AuthService } from './core/services/auth.service';
import { ThemeService } from './core/services/theme.service';

export function initAdminTheme(theme: ThemeService): () => void {
  return () => {
    theme.initFromStorage();
  };
}

/** Non-blocking: never hold bootstrap on profile/KYC HTTP (prevents frozen login UI). */
export function initAdminSession(auth: AuthService): () => void {
  return () => {
    auth.bootstrapFromStorage();
  };
}

@NgModule({
  declarations: [AppComponent, StaticShellPageComponent, MyAccountComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    CoreModule,
    SharedModule,
    UsersDialogsModule,
    AppRoutingModule,
  ],
  providers: [
    {
      provide: APP_INITIALIZER,
      useFactory: initAdminTheme,
      deps: [ThemeService],
      multi: true,
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initAdminSession,
      deps: [AuthService],
      multi: true,
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
