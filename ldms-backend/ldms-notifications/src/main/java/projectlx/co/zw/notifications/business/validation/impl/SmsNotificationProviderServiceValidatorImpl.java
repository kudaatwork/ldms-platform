package projectlx.co.zw.notifications.business.validation.impl;

import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

public class SmsNotificationProviderServiceValidatorImpl extends NotificationProviderServiceValidatorImpl {

    public SmsNotificationProviderServiceValidatorImpl(MessageService messageService) {
        super(messageService, Channel.SMS);
    }
}