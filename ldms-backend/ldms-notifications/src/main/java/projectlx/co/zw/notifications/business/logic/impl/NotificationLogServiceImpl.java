package projectlx.co.zw.notifications.business.logic.impl;

import com.lowagie.text.DocumentException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.co.zw.notifications.business.logic.api.NotificationLogService;
import projectlx.co.zw.notifications.business.validation.api.NotificationLogServiceValidator;
import projectlx.co.zw.notifications.model.NotificationLog;
import projectlx.co.zw.notifications.repository.NotificationLogRepository;
import projectlx.co.zw.notifications.repository.specification.NotificationLogSpecification;
import projectlx.co.zw.notifications.utils.dtos.NotificationLogDto;
import projectlx.co.zw.notifications.utils.dtos.NotificationQueueSummaryDto;
import projectlx.co.zw.notifications.utils.requests.NotificationLogMultipleFiltersRequest;
import projectlx.co.zw.notifications.utils.responses.NotificationLogResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.export.LdmsExcelReportWriter;
import projectlx.co.zw.shared_library.utils.export.LdmsExportReport;
import projectlx.co.zw.shared_library.utils.export.LdmsPdfReportWriter;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

public class NotificationLogServiceImpl implements NotificationLogService {

    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String[] EXPORT_HEADERS = {
            "ID", "EVENT ID", "RECIPIENT", "CHANNEL", "TEMPLATE KEY", "STATUS", "PROVIDER", "ERROR", "CREATED AT"
    };

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationLogServiceValidator notificationLogServiceValidator;
    private final RabbitAdmin rabbitAdmin;

    public NotificationLogServiceImpl(
            NotificationLogRepository notificationLogRepository,
            NotificationLogServiceValidator notificationLogServiceValidator,
            RabbitAdmin rabbitAdmin) {
        this.notificationLogRepository = notificationLogRepository;
        this.notificationLogServiceValidator = notificationLogServiceValidator;
        this.rabbitAdmin = rabbitAdmin;
    }

    @Value("${notifications.rabbitmq.queue:notifications.queue}")
    private String queueName;

    @Value("${notifications.rabbitmq.exchange:notifications.direct}")
    private String exchangeName;

    @Value("${notifications.rabbitmq.routing-key:notifications.send}")
    private String routingKey;

    @Override
    public NotificationLogResponse findByMultipleFilters(
            NotificationLogMultipleFiltersRequest request, String username, Locale locale) {

        ValidatorDto validatorDto = notificationLogServiceValidator.isNotificationLogMultipleFiltersRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            return buildResponse(400, false, "Invalid filter request.", null, null, validatorDto.getErrorMessages());
        }

        Specification<NotificationLog> spec = Specification.where(NotificationLogSpecification.notDeleted());
        spec = addToSpec(request.getTemplateKey(), spec, NotificationLogSpecification::templateKeyLike);
        spec = addToSpec(request.getStatus(), spec, NotificationLogSpecification::statusEquals);
        spec = addToSpec(request.getRecipientId(), spec, NotificationLogSpecification::recipientIdEquals);
        spec = addToSpec(request.getChannel(), spec, NotificationLogSpecification::channelEquals);
        spec = addToSpec(request.getProvider(), spec, NotificationLogSpecification::providerLike);
        if (StringUtils.hasText(request.getSearchValue())) {
            spec = addToSpec(request.getSearchValue(), spec, NotificationLogSpecification::any);
        }

        LocalDateTime from = parseDateTime(request.getFrom());
        LocalDateTime to = parseDateTime(request.getTo());
        if (from != null || to != null) {
            final LocalDateTime fromFinal = from;
            final LocalDateTime toFinal = to;
            Specification<NotificationLog> dateSpec = NotificationLogSpecification.createdBetween(fromFinal, toFinal);
            spec = spec == null ? dateSpec : spec.and(dateSpec);
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationLog> page = notificationLogRepository.findAll(spec, pageable);
        Page<NotificationLogDto> dtoPage = page.map(this::toDto);

        NotificationQueueSummaryDto queueSummary = loadQueueSummary();
        return buildResponse(200, true, "Notification log retrieved.", dtoPage, queueSummary, null);
    }

    @Override
    public byte[] exportToCsv(NotificationLogMultipleFiltersRequest request, String username, Locale locale) {
        List<NotificationLogDto> rows = loadExportRows(request, username, locale);
        String header = "id,eventId,recipientDisplay,channel,templateKey,status,provider,errorMessage,createdAt";
        String body = rows.stream().map(this::toCsvLine).collect(Collectors.joining("\n"));
        return (header + "\n" + body).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(NotificationLogMultipleFiltersRequest request, String username, Locale locale)
            throws IOException {
        List<NotificationLogDto> rows = loadExportRows(request, username, locale);
        return LdmsExcelReportWriter.write("Delivery Log", buildExportReport(rows));
    }

    @Override
    public byte[] exportToPdf(NotificationLogMultipleFiltersRequest request, String username, Locale locale)
            throws DocumentException {
        List<NotificationLogDto> rows = loadExportRows(request, username, locale);
        return LdmsPdfReportWriter.write(buildExportReport(rows));
    }

    @Override
    @Transactional
    public NotificationLogResponse churnOutLogs(String username, Locale locale) {
        int affected = notificationLogRepository.softDeleteAll(EntityStatus.DELETED, LocalDateTime.now());
        String message = affected > 0
                ? String.format("Notification logs churn out completed. %d records marked deleted.", affected)
                : "Notification logs churn out completed. No records matched.";
        return buildResponse(200, true, message, null, loadQueueSummary(), null);
    }

    @Override
    @Transactional
    public int churnOutLogsBefore(LocalDateTime cutoff, String username) {
        if (cutoff == null) {
            return 0;
        }
        return notificationLogRepository.softDeleteBefore(cutoff, EntityStatus.DELETED, LocalDateTime.now());
    }

    private List<NotificationLogDto> loadExportRows(
            NotificationLogMultipleFiltersRequest request, String username, Locale locale) {
        NotificationLogMultipleFiltersRequest exportRequest = request;
        exportRequest.setPage(0);
        exportRequest.setSize(10_000);
        NotificationLogResponse response = findByMultipleFilters(exportRequest, username, locale);
        Page<NotificationLogDto> page = response.getNotificationLogPage();
        return page != null ? page.getContent() : List.of();
    }

    private LdmsExportReport buildExportReport(List<NotificationLogDto> rows) {
        List<String[]> tableRows = new ArrayList<>();
        for (NotificationLogDto row : rows) {
            tableRows.add(new String[]{
                    row.getId() != null ? String.valueOf(row.getId()) : "",
                    nullToEmpty(row.getEventId()),
                    nullToEmpty(row.getRecipientDisplay()),
                    row.getChannel() != null ? row.getChannel().name() : "",
                    nullToEmpty(row.getTemplateKey()),
                    nullToEmpty(row.getStatus()),
                    nullToEmpty(row.getProvider()),
                    nullToEmpty(row.getErrorMessage()),
                    row.getCreatedAt() != null ? CSV_TIME.format(row.getCreatedAt()) : ""
            });
        }
        return LdmsExportReport.builder()
                .title("Notification Delivery Log")
                .reportCode("NTF-LOG")
                .subtitle("Outbound notification activity and delivery outcomes")
                .columnHeaders(EXPORT_HEADERS)
                .rows(tableRows)
                .landscape(true)
                .build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private NotificationQueueSummaryDto loadQueueSummary() {
        NotificationQueueSummaryDto summary = new NotificationQueueSummaryDto();
        summary.setQueueName(queueName);
        summary.setExchangeName(exchangeName);
        summary.setRoutingKey(routingKey);
        summary.setMessagesReady(0);
        summary.setMessagesUnacked(0);
        if (rabbitAdmin == null) {
            return summary;
        }
        try {
            Properties props = rabbitAdmin.getQueueProperties(queueName);
            if (props != null) {
                Object ready = props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT);
                if (ready instanceof Number number) {
                    summary.setMessagesReady(number.intValue());
                }
            }
        } catch (Exception ignored) {
            // Queue may not exist when notifications service starts before RabbitMQ wiring.
        }
        return summary;
    }

    private NotificationLogDto toDto(NotificationLog entity) {
        NotificationLogDto dto = new NotificationLogDto();
        dto.setId(entity.getId());
        dto.setEventId(entity.getEventId());
        dto.setRecipientId(entity.getRecipientId());
        dto.setRecipientEmail(entity.getRecipientEmail());
        dto.setRecipientPhone(entity.getRecipientPhone());
        dto.setRecipientDisplay(resolveRecipientDisplay(entity));
        dto.setTemplateKey(entity.getTemplateKey());
        dto.setChannel(entity.getChannel());
        dto.setStatus(entity.getStatus());
        dto.setProvider(entity.getProvider());
        dto.setProviderMessageId(entity.getProviderMessageId());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private static String resolveRecipientDisplay(NotificationLog entity) {
        if (StringUtils.hasText(entity.getRecipientEmail())) {
            return entity.getRecipientEmail();
        }
        if (StringUtils.hasText(entity.getRecipientPhone())) {
            return entity.getRecipientPhone();
        }
        if (StringUtils.hasText(entity.getRecipientId())) {
            return "user:" + entity.getRecipientId();
        }
        return "—";
    }

    private String toCsvLine(NotificationLogDto row) {
        return String.join(",",
                csv(row.getId()),
                csv(row.getEventId()),
                csv(row.getRecipientDisplay()),
                csv(row.getChannel() != null ? row.getChannel().name() : ""),
                csv(row.getTemplateKey()),
                csv(row.getStatus()),
                csv(row.getProvider()),
                csv(row.getErrorMessage()),
                csv(row.getCreatedAt() != null ? CSV_TIME.format(row.getCreatedAt()) : ""));
    }

    private static String csv(Object value) {
        String s = value == null ? "" : value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private Specification<NotificationLog> addToSpec(
            final String value,
            Specification<NotificationLog> spec,
            Function<String, Specification<NotificationLog>> predicateMethod) {
        if (StringUtils.hasText(value)) {
            Specification<NotificationLog> localSpec = Specification.where(predicateMethod.apply(value.trim()));
            return spec == null ? localSpec : spec.and(localSpec);
        }
        return spec;
    }

    private static LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private NotificationLogResponse buildResponse(
            int statusCode,
            boolean success,
            String message,
            Page<NotificationLogDto> page,
            NotificationQueueSummaryDto queueSummary,
            List<String> errors) {
        NotificationLogResponse response = new NotificationLogResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(success);
        response.setMessage(message);
        response.setNotificationLogPage(page);
        response.setQueueSummary(queueSummary);
        response.setErrorMessages(errors);
        return response;
    }
}
