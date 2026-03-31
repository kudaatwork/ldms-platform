package projectlx.co.zw.notifications.business.validation.impl;

import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

public class InAppNotificationProviderServiceValidatorImpl extends NotificationProviderServiceValidatorImpl {

    public InAppNotificationProviderServiceValidatorImpl(MessageService messageService) {
        super(messageService, Channel.IN_APP);
    }
}