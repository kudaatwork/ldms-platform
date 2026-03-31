import { NgModule } from '@angular/core';
import { ActivityRoutingModule } from './activity-routing.module';
import { ActivityPageComponent } from './pages/activity-page/activity-page.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [ActivityPageComponent],
  imports: [SharedModule, ActivityRoutingModule],
})
export class ActivityModule {}
