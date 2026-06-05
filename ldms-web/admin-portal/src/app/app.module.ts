import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { StaticShellPageComponent } from './shared/static-shell-page/static-shell-page.component';
import { CoreModule } from './core/core.module';
import { SharedModule } from './shared/shared.module';
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
  declarations: [AppComponent, StaticShellPageComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    CoreModule,
    SharedModule,
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
