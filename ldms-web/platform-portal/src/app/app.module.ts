import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CoreModule } from './core/core.module';
import { SharedModule } from './shared/shared.module';
import { ShellLayoutComponent } from './layout/shell-layout/shell-layout.component';
import { PlaceholderPageComponent } from './features/portal/pages/placeholder-page/placeholder-page.component';
import { LandingModule } from './features/landing/landing.module';
import { ContactDemoComponent } from './features/contact/pages/contact-demo/contact-demo.component';
import { OfflinePageComponent } from './shared/components/offline-page/offline-page.component';
import { LxRouteProgressComponent } from './shared/components/lx-route-progress/lx-route-progress.component';
import { AuthService } from './core/services/auth.service';
import { ThemeService } from './core/services/theme.service';

export function initPlatformTheme(theme: ThemeService): () => void {
  return () => {
    theme.initFromStorage();
  };
}

/** Non-blocking: never hold bootstrap on profile/org HTTP (prevents frozen login UI). */
export function initPlatformSession(auth: AuthService): () => void {
  return () => {
    auth.bootstrapFromStorage();
  };
}

@NgModule({
  declarations: [
    AppComponent,
    ShellLayoutComponent,
    PlaceholderPageComponent,
    ContactDemoComponent,
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    CoreModule,
    SharedModule,
    LandingModule,
    AppRoutingModule,
    OfflinePageComponent,
    LxRouteProgressComponent,
  ],
  providers: [
    {
      provide: APP_INITIALIZER,
      useFactory: initPlatformTheme,
      deps: [ThemeService],
      multi: true,
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initPlatformSession,
      deps: [AuthService],
      multi: true,
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
