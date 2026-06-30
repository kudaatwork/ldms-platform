import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { LandingShellComponent } from './components/landing-shell/landing-shell.component';
import { LandingComponent } from './pages/landing/landing.component';
import { PricingPageComponent } from './pages/pricing/pricing-page.component';
import { AboutPageComponent } from './pages/about/about-page.component';
import { DemoPageComponent } from './pages/demo/demo-page.component';
import { SupportPageComponent } from './pages/support/support-page.component';
import { SystemDemoPlayerComponent } from './components/system-demo-player/system-demo-player.component';

@NgModule({
  declarations: [
    LandingShellComponent,
    LandingComponent,
    PricingPageComponent,
    AboutPageComponent,
    DemoPageComponent,
    SupportPageComponent,
    SystemDemoPlayerComponent,
  ],
  imports: [CommonModule, RouterModule, SharedModule],
  exports: [
    LandingShellComponent,
    LandingComponent,
    PricingPageComponent,
    AboutPageComponent,
    DemoPageComponent,
    SupportPageComponent,
  ],
})
export class LandingModule {}
