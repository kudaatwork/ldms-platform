package projectlx.messaging.inbound.business.logic.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.messaging.inbound.business.logic.api.BotAnalyticsService;
import projectlx.messaging.inbound.repository.BotFaqRepository;
import projectlx.messaging.inbound.repository.BotMessageRepository;
import projectlx.messaging.inbound.repository.BotSessionRepository;
import projectlx.messaging.inbound.repository.projection.BotMessageDailyCountProjection;
import projectlx.messaging.inbound.repository.projection.BotSessionDailyCountProjection;
import projectlx.messaging.inbound.repository.projection.BotTopicCountProjection;
import projectlx.messaging.inbound.utils.dtos.BotAnalyticsChannelCountDto;
import projectlx.messaging.inbound.utils.dtos.BotAnalyticsDailyCountDto;
import projectlx.messaging.inbound.utils.dtos.BotAnalyticsMessageDailyCountDto;
import projectlx.messaging.inbound.utils.dtos.BotAnalyticsSummaryDto;
import projectlx.messaging.inbound.utils.dtos.BotAnalyticsTopicCountDto;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;
import projectlx.messaging.inbound.utils.enums.I18Code;
import projectlx.messaging.inbound.utils.responses.BotAnalyticsResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BotAnalyticsServiceImpl implements BotAnalyticsService {

    private final BotSessionRepository botSessionRepository;
    private final BotMessageRepository botMessageRepository;
    private final BotFaqRepository botFaqRepository;
    private final MessageService messageService;

    @Override
    public BotAnalyticsResponse getSummary(int days, Locale locale) {
        int windowDays = Math.max(1, Math.min(days, 90));
        EntityStatus excluded = EntityStatus.DELETED;
        LocalDateTime since = LocalDate.now().minusDays(windowDays - 1L).atStartOfDay();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();

        long totalSessions = botSessionRepository.countByEntityStatusNot(excluded);
        long sessionsToday = botSessionRepository.countByEntityStatusNotAndCreatedAtGreaterThanEqual(
                excluded, todayStart);
        long sessionsLast7Days = botSessionRepository.countByEntityStatusNotAndCreatedAtGreaterThanEqual(
                excluded, weekStart);

        long totalMessages = botMessageRepository.countByEntityStatusNot(excluded);
        long userMessages = botMessageRepository.countByRoleAndEntityStatusNot(BotMessageRole.USER, excluded);
        long botMessages = botMessageRepository.countByRoleAndEntityStatusNot(BotMessageRole.BOT, excluded);

        Double avgScore = botSessionRepository.averageSatisfactionScore(excluded);
        long ratedCount = botSessionRepository.countByEntityStatusNotAndSatisfactionScoreIsNotNull(excluded);

        BigDecimal averageMessagesPerSession = totalSessions == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalMessages)
                .divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);

        BigDecimal averageSatisfaction = avgScore == null
                ? null
                : BigDecimal.valueOf(avgScore).setScale(2, RoundingMode.HALF_UP);

        BotAnalyticsSummaryDto summary = new BotAnalyticsSummaryDto();
        summary.setTotalSessions(totalSessions);
        summary.setSessionsToday(sessionsToday);
        summary.setSessionsLast7Days(sessionsLast7Days);
        summary.setTotalMessages(totalMessages);
        summary.setUserMessages(userMessages);
        summary.setBotMessages(botMessages);
        summary.setAverageMessagesPerSession(averageMessagesPerSession);
        summary.setAverageSatisfactionScore(averageSatisfaction);
        summary.setRatedSessionCount(ratedCount);
        summary.setPublishedFaqCount(botFaqRepository.countByPublishedTrueAndEntityStatusNot(excluded));
        summary.setSessionsByDay(mapSessionDailyCounts(since));
        summary.setMessagesByDay(mapMessageDailyCounts(since));
        summary.setTopTopics(mapTopTopics());
        summary.setChannelBreakdown(mapChannelBreakdown(excluded));

        BotAnalyticsResponse response = new BotAnalyticsResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(
                I18Code.MESSAGE_BOT_ANALYTICS_RETRIEVED.getCode(), new String[]{}, locale));
        response.setAnalyticsSummary(summary);
        return response;
    }

    private List<BotAnalyticsDailyCountDto> mapSessionDailyCounts(LocalDateTime since) {
        List<BotAnalyticsDailyCountDto> rows = new ArrayList<>();
        for (BotSessionDailyCountProjection row : botSessionRepository.countSessionsGroupedByDay(since)) {
            BotAnalyticsDailyCountDto dto = new BotAnalyticsDailyCountDto();
            dto.setDate(row.getDay() != null ? row.getDay().toString() : "");
            dto.setCount(nullToLong(row.getCount()));
            rows.add(dto);
        }
        return rows;
    }

    private List<BotAnalyticsMessageDailyCountDto> mapMessageDailyCounts(LocalDateTime since) {
        List<BotAnalyticsMessageDailyCountDto> rows = new ArrayList<>();
        for (BotMessageDailyCountProjection row : botMessageRepository.countMessagesGroupedByDay(since)) {
            BotAnalyticsMessageDailyCountDto dto = new BotAnalyticsMessageDailyCountDto();
            dto.setDate(row.getDay() != null ? row.getDay().toString() : "");
            dto.setUserMessages(nullToLong(row.getUserMessages()));
            dto.setBotMessages(nullToLong(row.getBotMessages()));
            rows.add(dto);
        }
        return rows;
    }

    private List<BotAnalyticsTopicCountDto> mapTopTopics() {
        List<BotAnalyticsTopicCountDto> rows = new ArrayList<>();
        for (BotTopicCountProjection row : botSessionRepository.findTopTopics()) {
            BotAnalyticsTopicCountDto dto = new BotAnalyticsTopicCountDto();
            dto.setTopic(row.getTopic());
            dto.setCount(nullToLong(row.getTopicCount()));
            rows.add(dto);
        }
        return rows;
    }

    private static long nullToLong(Long value) {
        return value == null ? 0L : value;
    }

    private List<BotAnalyticsChannelCountDto> mapChannelBreakdown(EntityStatus excluded) {
        List<BotAnalyticsChannelCountDto> rows = new ArrayList<>();
        for (Object[] row : botSessionRepository.countByChannel(excluded)) {
            BotAnalyticsChannelCountDto dto = new BotAnalyticsChannelCountDto();
            dto.setChannel(String.valueOf(row[0]));
            dto.setCount(((Number) row[1]).longValue());
            rows.add(dto);
        }
        return rows;
    }
}
