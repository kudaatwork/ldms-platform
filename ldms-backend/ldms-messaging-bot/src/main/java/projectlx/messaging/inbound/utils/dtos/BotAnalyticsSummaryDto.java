package projectlx.messaging.inbound.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotAnalyticsSummaryDto {
    private long totalSessions;
    private long sessionsToday;
    private long sessionsLast7Days;
    private long totalMessages;
    private long userMessages;
    private long botMessages;
    private BigDecimal averageMessagesPerSession;
    private BigDecimal averageSatisfactionScore;
    private long ratedSessionCount;
    private long publishedFaqCount;
    private List<BotAnalyticsDailyCountDto> sessionsByDay;
    private List<BotAnalyticsMessageDailyCountDto> messagesByDay;
    private List<BotAnalyticsTopicCountDto> topTopics;
    private List<BotAnalyticsChannelCountDto> channelBreakdown;
}
