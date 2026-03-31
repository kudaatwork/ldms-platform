import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { UiKitRoutingModule } from './ui-kit-routing.module';
import { UiKitComponent } from './pages/ui-kit/ui-kit.component';
import { UiKitDemoDialogComponent } from './pages/ui-kit/ui-kit-demo-dialog.component';

@NgModule({
  declarations: [UiKitComponent, UiKitDemoDialogComponent],
  imports: [SharedModule, UiKitRoutingModule],
})
export class UiKitModule {}

