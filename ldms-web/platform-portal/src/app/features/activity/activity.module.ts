import { NgModule } from '@angular/core';
import { ActivityRoutingModule } from './activity-routing.module';
import { AuditLogRequestDetailDialogComponent } from './components/audit-log-request-detail-dialog/audit-log-request-detail-dialog.component';
import { LoginAnalyticsPageComponent } from './pages/login-analytics-page/login-analytics-page.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [LoginAnalyticsPageComponent, AuditLogRequestDetailDialogComponent],
  imports: [SharedModule, ActivityRoutingModule],
})
export class ActivityModule {}
