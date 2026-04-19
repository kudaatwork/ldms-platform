import { NgModule } from '@angular/core';
import { NotificationTemplateDetailDialogComponent } from './components/notification-template-detail-dialog/notification-template-detail-dialog.component';
import { NotificationTemplateFormDialogComponent } from './components/notification-template-form-dialog/notification-template-form-dialog.component';
import { NotificationsRoutingModule } from './notifications-routing.module';
import { NotificationsComponent } from './pages/notifications/notifications.component';
import { SharedModule } from '@shared/shared.module';

@NgModule({
  declarations: [
    NotificationsComponent,
    NotificationTemplateDetailDialogComponent,
    NotificationTemplateFormDialogComponent,
  ],
  imports: [SharedModule, NotificationsRoutingModule],
})
export class NotificationsModule {}
