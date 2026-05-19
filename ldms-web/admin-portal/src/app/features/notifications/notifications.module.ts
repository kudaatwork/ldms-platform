import { NgModule } from '@angular/core';
import { NotificationEmailHtmlPadComponent } from './components/notification-email-html-pad/notification-email-html-pad.component';
import { NotificationEmailHtmlPreviewComponent } from './components/notification-email-html-preview/notification-email-html-preview.component';
import { NotificationLogDetailDialogComponent } from './components/notification-log-detail-dialog/notification-log-detail-dialog.component';
import { NotificationTemplateDetailDialogComponent } from './components/notification-template-detail-dialog/notification-template-detail-dialog.component';
import { NotificationTemplateFormDialogComponent } from './components/notification-template-form-dialog/notification-template-form-dialog.component';
import { NotificationsRoutingModule } from './notifications-routing.module';
import { NotificationsComponent } from './pages/notifications/notifications.component';
import { SharedModule } from '@shared/shared.module';

@NgModule({
  declarations: [
    NotificationsComponent,
    NotificationEmailHtmlPadComponent,
    NotificationEmailHtmlPreviewComponent,
    NotificationLogDetailDialogComponent,
    NotificationTemplateDetailDialogComponent,
    NotificationTemplateFormDialogComponent,
  ],
  imports: [SharedModule, NotificationsRoutingModule],
})
export class NotificationsModule {}
