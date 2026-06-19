package projectlx.messaging.inbound.service.processor.impl;

import lombok.RequiredArgsConstructor;
import projectlx.messaging.inbound.business.logic.api.BotFaqService;
import projectlx.messaging.inbound.service.processor.api.BotFaqServiceProcessor;
import projectlx.messaging.inbound.utils.requests.CreateBotFaqRequest;
import projectlx.messaging.inbound.utils.requests.EditBotFaqRequest;
import projectlx.messaging.inbound.utils.responses.BotFaqResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class BotFaqServiceProcessorImpl implements BotFaqServiceProcessor {

    private final BotFaqService botFaqService;

    @Override
    public BotFaqResponse listAll(Locale locale) {
        return botFaqService.listAll(locale);
    }

    @Override
    public BotFaqResponse findById(Long id, Locale locale) {
        return botFaqService.findById(id, locale);
    }

    @Override
    public BotFaqResponse create(CreateBotFaqRequest request, Locale locale, String username) {
        return botFaqService.create(request, locale, username);
    }

    @Override
    public BotFaqResponse update(Long id, EditBotFaqRequest request, Locale locale, String username) {
        return botFaqService.update(id, request, locale, username);
    }

    @Override
    public BotFaqResponse delete(Long id, Locale locale, String username) {
        return botFaqService.delete(id, locale, username);
    }
}
