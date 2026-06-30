package projectlx.co.zw.notifications.business.logic.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.co.zw.notifications.business.auditable.api.PlatformUserNotificationServiceAuditable;
import projectlx.co.zw.notifications.business.logic.api.PlatformUserNotificationService;
import projectlx.co.zw.notifications.model.PlatformUserNotification;
import projectlx.co.zw.notifications.repository.PlatformUserNotificationRepository;
import projectlx.co.zw.notifications.utils.dtos.PlatformUserNotificationDto;
import projectlx.co.zw.notifications.utils.responses.PlatformUserNotificationResponse;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.requests.PlatformBellNotificationRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class PlatformUserNotificationServiceImpl implements PlatformUserNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PlatformUserNotificationServiceImpl.class);
    private static final int INBOX_LIMIT = 50;

    private final PlatformUserNotificationRepository repository;
    private final PlatformUserNotificationServiceAuditable auditable;

    public PlatformUserNotificationServiceImpl(
            PlatformUserNotificationRepository repository,
            PlatformUserNotificationServiceAuditable auditable) {
        this.repository = repository;
        this.auditable = auditable;
    }

    @Override
    public PlatformUserNotificationResponse ingest(PlatformBellNotificationRequest request, Locale locale) {
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            return buildResponse(400, false, "Invalid notification recipient", null, null);
        }
        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getBody())) {
            return buildResponse(400, false, "Notification title and body are required", null, null);
        }

        String sourceEventId = StringUtils.hasText(request.getEventId())
                ? request.getEventId().trim()
                : UUID.randomUUID().toString();

        Optional<PlatformUserNotification> existing = repository.findByUserIdAndSourceEventIdAndEntityStatusNot(
                request.getUserId(), sourceEventId, EntityStatus.DELETED);
        if (existing.isPresent()) {
            log.debug("[PLATFORM-BELL] Duplicate skipped userId={} sourceEventId={}", request.getUserId(), sourceEventId);
            return buildResponse(200, true, "Notification already recorded", mapToDto(existing.get()), null);
        }

        PlatformUserNotification notification = new PlatformUserNotification();
        notification.setUserId(request.getUserId());
        notification.setOrganizationId(request.getOrganizationId());
        notification.setEventKey(StringUtils.hasText(request.getEventKey()) ? request.getEventKey().trim() : "GENERAL");
        notification.setTitle(request.getTitle().trim());
        notification.setBody(request.getBody().trim());
        notification.setActionRoute(request.getActionRoute());
        notification.setEntityType(request.getEntityType());
        notification.setEntityId(request.getEntityId());
        notification.setSourceEventId(sourceEventId);
        notification.setCreatedBy(StringUtils.hasText(request.getSourceService()) ? request.getSourceService() : "system");

        PlatformUserNotification saved = auditable.create(notification, locale, notification.getCreatedBy());
        log.info("[PLATFORM-BELL] Stored id={} userId={} eventKey={}", saved.getId(), saved.getUserId(), saved.getEventKey());
        return buildResponse(201, true, "Notification stored", mapToDto(saved), null);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformUserNotificationResponse listInbox(Long userId, Locale locale, String username) {
        if (userId == null || userId <= 0) {
            return buildResponse(400, false, "Signed-in user id is required", null, null);
        }
        List<PlatformUserNotification> rows = repository
                .findByUserIdAndDismissedAtIsNullAndEntityStatusNotOrderByCreatedAtDesc(
                        userId, EntityStatus.DELETED, PageRequest.of(0, INBOX_LIMIT));
        List<PlatformUserNotificationDto> dtos = new ArrayList<>();
        for (PlatformUserNotification row : rows) {
            dtos.add(mapToDto(row));
        }
        return buildResponse(200, true, "Inbox retrieved", null, dtos);
    }

    @Override
    public PlatformUserNotificationResponse dismiss(Long userId, Long notificationId, Locale locale, String username) {
        if (userId == null || userId <= 0 || notificationId == null || notificationId <= 0) {
            return buildResponse(400, false, "Invalid dismiss request", null, null);
        }
        Optional<PlatformUserNotification> rowOpt = repository.findByIdAndUserIdAndEntityStatusNot(
                notificationId, userId, EntityStatus.DELETED);
        if (rowOpt.isEmpty()) {
            return buildResponse(404, false, "Notification not found", null, null);
        }
        PlatformUserNotification row = rowOpt.get();
        LocalDateTime now = LocalDateTime.now();
        row.setDismissedAt(now);
        if (row.getReadAt() == null) {
            row.setReadAt(now);
        }
        auditable.update(row, locale, username);
        return buildResponse(200, true, "Notification dismissed", mapToDto(row), null);
    }

    @Override
    public PlatformUserNotificationResponse dismissAll(Long userId, Locale locale, String username) {
        if (userId == null || userId <= 0) {
            return buildResponse(400, false, "Signed-in user id is required", null, null);
        }
        List<PlatformUserNotification> rows = repository.findByUserIdAndDismissedAtIsNullAndEntityStatusNot(
                userId, EntityStatus.DELETED);
        LocalDateTime now = LocalDateTime.now();
        for (PlatformUserNotification row : rows) {
            row.setDismissedAt(now);
            if (row.getReadAt() == null) {
                row.setReadAt(now);
            }
            auditable.update(row, locale, username);
        }
        return buildResponse(200, true, "All notifications dismissed", null, null);
    }

    private PlatformUserNotificationDto mapToDto(PlatformUserNotification row) {
        PlatformUserNotificationDto dto = new PlatformUserNotificationDto();
        dto.setId(row.getId());
        dto.setUserId(row.getUserId());
        dto.setOrganizationId(row.getOrganizationId());
        dto.setEventKey(row.getEventKey());
        dto.setTitle(row.getTitle());
        dto.setBody(row.getBody());
        dto.setActionRoute(row.getActionRoute());
        dto.setEntityType(row.getEntityType());
        dto.setEntityId(row.getEntityId());
        dto.setSourceEventId(row.getSourceEventId());
        dto.setReadAt(row.getReadAt());
        dto.setDismissedAt(row.getDismissedAt());
        dto.setCreatedAt(row.getCreatedAt());
        dto.setUnread(row.getReadAt() == null && row.getDismissedAt() == null);
        return dto;
    }

    private PlatformUserNotificationResponse buildResponse(
            int statusCode,
            boolean success,
            String message,
            PlatformUserNotificationDto dto,
            List<PlatformUserNotificationDto> dtoList) {
        PlatformUserNotificationResponse response = new PlatformUserNotificationResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(success);
        response.setMessage(message);
        response.setNotificationDto(dto);
        response.setNotificationDtoList(dtoList);
        return response;
    }
}
