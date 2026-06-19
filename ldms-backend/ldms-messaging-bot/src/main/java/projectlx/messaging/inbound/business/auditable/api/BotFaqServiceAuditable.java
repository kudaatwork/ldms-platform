package projectlx.messaging.inbound.business.auditable.api;

import projectlx.messaging.inbound.model.BotFaq;

import java.util.Locale;

public interface BotFaqServiceAuditable {

    BotFaq create(BotFaq faq, Locale locale, String username);

    BotFaq update(BotFaq faq, Locale locale, String username);

    BotFaq delete(BotFaq faq, Locale locale, String username);
}
