import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CoreModule } from './core/core.module';
import { SharedModule } from './shared/shared.module';
import { ShellLayoutComponent } from './layout/shell-layout/shell-layout.component';
import { PlaceholderPageComponent } from './features/portal/pages/placeholder-page/placeholder-page.component';

@NgModule({
  declarations: [AppComponent, ShellLayoutComponent, PlaceholderPageComponent],
  imports: [
    BrowserModule,
    HttpClientModule,
    CoreModule,
    SharedModule,
    AppRoutingModule,
  ],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
