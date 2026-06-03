import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { firstValueFrom, of } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';

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

export function initAdminSession(auth: AuthService): () => Promise<void> {
  return () =>
    firstValueFrom(
      auth.initializeSession().pipe(
        timeout(8000),
        catchError(() => of(undefined)),
      ),
    ).then(
      () => undefined,
      () => undefined,
    );
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
