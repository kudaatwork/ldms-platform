package projectlx.messaging.inbound.service.processor.api;

import projectlx.messaging.inbound.utils.responses.BotAnalyticsResponse;

import java.util.Locale;

public interface BotAnalyticsServiceProcessor {

    BotAnalyticsResponse getSummary(int days, Locale locale);
}
