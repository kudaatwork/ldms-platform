import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CoreModule } from './core/core.module';
import { SharedModule } from './shared/shared.module';
import { ShellLayoutComponent } from './layout/shell-layout/shell-layout.component';
import { PlaceholderPageComponent } from './features/portal/pages/placeholder-page/placeholder-page.component';
import { LandingComponent } from './features/landing/pages/landing/landing.component';
import { ContactDemoComponent } from './features/contact/pages/contact-demo/contact-demo.component';
import { AuthService } from './core/services/auth.service';
import { ThemeService } from './core/services/theme.service';

export function initPlatformTheme(theme: ThemeService): () => void {
  return () => {
    theme.initFromStorage();
  };
}

export function initPlatformSession(auth: AuthService): () => Promise<void> {
  return () =>
    firstValueFrom(auth.initializeSession()).then(
      () => undefined,
      () => undefined,
    );
}

@NgModule({
  declarations: [
    AppComponent,
    ShellLayoutComponent,
    PlaceholderPageComponent,
    LandingComponent,
    ContactDemoComponent,
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    CoreModule,
    SharedModule,
    AppRoutingModule,
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
