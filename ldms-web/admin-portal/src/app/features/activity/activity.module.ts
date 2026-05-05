import { NgModule } from '@angular/core';
import { ActivityRoutingModule } from './activity-routing.module';
import { AuditLogRequestDetailDialogComponent } from './components/audit-log-request-detail-dialog/audit-log-request-detail-dialog.component';
import { ChurnOutConfirmDialogComponent } from './components/churn-out-confirm-dialog/churn-out-confirm-dialog.component';
import { ActivityPageComponent } from './pages/activity-page/activity-page.component';
import { ChurnoutHistoryPageComponent } from './pages/churnout-history-page/churnout-history-page.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [
    ActivityPageComponent,
    AuditLogRequestDetailDialogComponent,
    ChurnOutConfirmDialogComponent,
    ChurnoutHistoryPageComponent,
  ],
  imports: [SharedModule, ActivityRoutingModule],
})
export class ActivityModule {}
