package projectlx.co.zw.notifications.business.validation.impl;

import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

public class WhatsAppNotificationProviderServiceValidatorImpl extends NotificationProviderServiceValidatorImpl {

    public WhatsAppNotificationProviderServiceValidatorImpl(MessageService messageService) {
        super(messageService, Channel.WHATSAPP);
    }
}