package projectlx.messaging.inbound.service.processor.impl;

import lombok.RequiredArgsConstructor;
import projectlx.messaging.inbound.business.logic.api.BotAnalyticsService;
import projectlx.messaging.inbound.service.processor.api.BotAnalyticsServiceProcessor;
import projectlx.messaging.inbound.utils.responses.BotAnalyticsResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class BotAnalyticsServiceProcessorImpl implements BotAnalyticsServiceProcessor {

    private final BotAnalyticsService botAnalyticsService;

    @Override
    public BotAnalyticsResponse getSummary(int days, Locale locale) {
        return botAnalyticsService.getSummary(days, locale);
    }
}
