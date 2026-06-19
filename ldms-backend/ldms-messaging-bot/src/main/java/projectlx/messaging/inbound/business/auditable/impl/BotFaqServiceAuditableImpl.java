package projectlx.messaging.inbound.business.auditable.impl;

import projectlx.messaging.inbound.business.auditable.api.BotFaqServiceAuditable;
import projectlx.messaging.inbound.model.BotFaq;
import projectlx.messaging.inbound.repository.BotFaqRepository;

import java.util.Locale;

public class BotFaqServiceAuditableImpl implements BotFaqServiceAuditable {

    private final BotFaqRepository botFaqRepository;

    public BotFaqServiceAuditableImpl(BotFaqRepository botFaqRepository) {
        this.botFaqRepository = botFaqRepository;
    }

    @Override
    public BotFaq create(BotFaq faq, Locale locale, String username) {
        return botFaqRepository.save(faq);
    }

    @Override
    public BotFaq update(BotFaq faq, Locale locale, String username) {
        return botFaqRepository.save(faq);
    }

    @Override
    public BotFaq delete(BotFaq faq, Locale locale, String username) {
        return botFaqRepository.save(faq);
    }
}
