package projectlx.co.zw.notificationsmanagementservice.business.validation.impl;

import projectlx.co.zw.notificationsmanagementservice.model.Channel;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

public class SmsNotificationProviderServiceValidatorImpl extends NotificationProviderServiceValidatorImpl {

    public SmsNotificationProviderServiceValidatorImpl(MessageService messageService) {
        super(messageService, Channel.SMS);
    }
}