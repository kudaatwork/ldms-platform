import { NgModule } from '@angular/core';
import { ActivityRoutingModule } from './activity-routing.module';
import { AuditLogRequestDetailDialogComponent } from './components/audit-log-request-detail-dialog/audit-log-request-detail-dialog.component';
import { ActivityPageComponent } from './pages/activity-page/activity-page.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [ActivityPageComponent, AuditLogRequestDetailDialogComponent],
  imports: [SharedModule, ActivityRoutingModule],
})
export class ActivityModule {}
