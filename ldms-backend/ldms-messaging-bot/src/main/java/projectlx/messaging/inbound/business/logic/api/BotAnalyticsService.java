package projectlx.messaging.inbound.business.logic.api;

import projectlx.messaging.inbound.utils.responses.BotAnalyticsResponse;

import java.util.Locale;

public interface BotAnalyticsService {

    BotAnalyticsResponse getSummary(int days, Locale locale);
}
