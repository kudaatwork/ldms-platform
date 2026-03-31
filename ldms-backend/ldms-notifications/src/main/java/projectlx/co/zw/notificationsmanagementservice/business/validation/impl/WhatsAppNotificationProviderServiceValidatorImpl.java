package projectlx.co.zw.notificationsmanagementservice.business.validation.impl;

import projectlx.co.zw.notificationsmanagementservice.model.Channel;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

public class WhatsAppNotificationProviderServiceValidatorImpl extends NotificationProviderServiceValidatorImpl {

    public WhatsAppNotificationProviderServiceValidatorImpl(MessageService messageService) {
        super(messageService, Channel.WHATSAPP);
    }
}