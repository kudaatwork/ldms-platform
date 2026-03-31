import { NgModule } from '@angular/core';
import { SystemRoutingModule } from './system-routing.module';
import { SystemHealthComponent } from './pages/system-health/system-health.component';
import { SystemMonitoringComponent } from './pages/system-monitoring/system-monitoring.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [SystemHealthComponent, SystemMonitoringComponent],
  imports: [SharedModule, SystemRoutingModule],
})
export class SystemModule {}
