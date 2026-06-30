package projectlx.trip.tracking.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;
import projectlx.trip.tracking.clients.FleetManagementServiceClient;
import projectlx.trip.tracking.clients.UserManagementServiceClient;
import projectlx.trip.tracking.model.Trip;
import projectlx.trip.tracking.model.TripMessage;
import projectlx.trip.tracking.repository.TripMessageRepository;
import projectlx.trip.tracking.repository.TripRepository;
import projectlx.trip.tracking.utils.dtos.FleetDriverSummaryDto;
import projectlx.trip.tracking.utils.dtos.ReceiverContactDto;
import projectlx.trip.tracking.utils.dtos.TripMessageDto;
import projectlx.trip.tracking.utils.responses.FleetDriverFeignResponse;
import projectlx.trip.tracking.utils.enums.TripMessageSenderRole;
import projectlx.trip.tracking.utils.requests.SendTripMessageRequest;
import projectlx.trip.tracking.utils.responses.ReceiverContactResponse;
import projectlx.trip.tracking.utils.responses.TripChatResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Driver ⇄ receiver in-app chat for a trip.
 *
 * <p>Both the assigned driver and the trip's receiver may read and post messages.
 * Authorisation resolves the caller against the trip: the assigned fleet driver
 * posts as {@code DRIVER}, the {@code receiverUserId} posts as {@code RECEIVER};
 * anyone else is rejected.
 */
@Service
@RequiredArgsConstructor
public class TripChatService {

    private static final Logger log = LoggerFactory.getLogger(TripChatService.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("d MMM HH:mm");
    private static final int MAX_BODY = 2000;

    private final TripRepository tripRepository;
    private final TripMessageRepository tripMessageRepository;
    private final TripDriverPortalSupport driverPortalSupport;
    private final UserManagementServiceClient userManagementServiceClient;
    private final FleetManagementServiceClient fleetManagementServiceClient;

    /** Resolved chat participant — never exposed beyond the service. */
    private record Participant(Long userId, TripMessageSenderRole role) {}

    // ============================================================
    // List messages
    // ============================================================
    @Transactional
    public TripChatResponse listMessages(Long tripId, Locale locale, String username) {
        Trip trip = tripRepository.findByIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return chatError(404, "Trip not found.");
        }
        Participant participant = resolveParticipant(trip, username, locale);
        if (participant == null) {
            return chatError(403, "You are not a participant on this trip's chat.");
        }

        // Mark inbound (other party's) messages as read.
        List<TripMessage> messages = tripMessageRepository
                .findByTripIdAndEntityStatusNotOrderByCreatedAtAsc(tripId, EntityStatus.DELETED);
        LocalDateTime now = LocalDateTime.now();
        for (TripMessage m : messages) {
            if (!m.getSenderUserId().equals(participant.userId()) && m.getReadAt() == null) {
                m.setReadAt(now);
            }
        }
        tripMessageRepository.saveAll(messages);

        TripChatResponse response = new TripChatResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessages(messages.stream().map(m -> toDto(m, participant.userId())).toList());
        response.setReceiverContact(buildCounterpartContact(trip, participant, locale));
        response.setMyRole(participant.role().name());
        response.setCurrentUserId(participant.userId());
        response.setUnreadCount(0);
        return response;
    }

    // ============================================================
    // Send a message
    // ============================================================
    @Transactional
    public TripChatResponse sendMessage(Long tripId, SendTripMessageRequest request, Locale locale, String username) {
        if (request == null || !StringUtils.hasText(request.getBody())) {
            return chatError(400, "Message cannot be empty.");
        }
        Trip trip = tripRepository.findByIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        if (trip == null) {
            return chatError(404, "Trip not found.");
        }
        Participant participant = resolveParticipant(trip, username, locale);
        if (participant == null) {
            return chatError(403, "You are not a participant on this trip's chat.");
        }

        String body = request.getBody().trim();
        if (body.length() > MAX_BODY) {
            body = body.substring(0, MAX_BODY);
        }

        LocalDateTime now = LocalDateTime.now();
        TripMessage message = new TripMessage();
        message.setTripId(tripId);
        message.setSenderUserId(participant.userId());
        message.setSenderRole(participant.role());
        message.setSenderName(resolveSenderName(participant, trip, locale));
        message.setBody(body);
        message.setEntityStatus(EntityStatus.ACTIVE);
        message.setCreatedAt(now);
        message.setCreatedBy(username);
        tripMessageRepository.save(message);

        return listMessages(tripId, locale, username);
    }

    // ============================================================
    // Receiver contact card
    // ============================================================
    @Transactional(readOnly = true)
    public ReceiverContactResponse getReceiverContact(Long tripId, Locale locale, String username) {
        Trip trip = tripRepository.findByIdAndEntityStatusNot(tripId, EntityStatus.DELETED).orElse(null);
        ReceiverContactResponse response = new ReceiverContactResponse();
        if (trip == null) {
            response.setSuccess(false);
            response.setStatusCode(404);
            response.setMessage("Trip not found.");
            return response;
        }
        Participant participant = resolveParticipant(trip, username, locale);
        if (participant == null) {
            response.setSuccess(false);
            response.setStatusCode(403);
            response.setMessage("You cannot view this trip's receiver.");
            return response;
        }
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setReceiverContact(buildCounterpartContact(trip, participant, locale));
        return response;
    }

    // ============================================================
    // Helpers
    // ============================================================
    private Participant resolveParticipant(Trip trip, String username, Locale locale) {
        Long sessionUserId = driverPortalSupport.resolveSessionUserId(username, locale);
        if (sessionUserId == null) {
            return null;
        }
        if (trip.getReceiverUserId() != null && sessionUserId.equals(trip.getReceiverUserId())) {
            return new Participant(sessionUserId, TripMessageSenderRole.RECEIVER);
        }
        Long fleetDriverId = driverPortalSupport.resolveFleetDriverId(username, locale);
        if (driverPortalSupport.isTripAccessibleToDriver(trip, fleetDriverId, sessionUserId, locale)) {
            return new Participant(sessionUserId, TripMessageSenderRole.DRIVER);
        }
        // A clerk in the trip's destination organisation receives the goods and may
        // chat with the driver even before an explicit receiver is bound at OTP time.
        if (trip.getOrganizationId() != null) {
            UserDto user = lookupUser(sessionUserId, locale);
            if (user != null && trip.getOrganizationId().equals(user.getOrganizationId())) {
                return new Participant(sessionUserId, TripMessageSenderRole.RECEIVER);
            }
        }
        return null;
    }

    /**
     * Contact card for the party the caller is talking <em>to</em>: a driver sees the
     * receiver/consignee, a receiver (or destination-org clerk) sees the driver.
     */
    private ReceiverContactDto buildCounterpartContact(Trip trip, Participant participant, Locale locale) {
        if (participant.role() == TripMessageSenderRole.RECEIVER) {
            return buildDriverContact(trip, locale);
        }
        return buildReceiverContact(trip, locale);
    }

    private ReceiverContactDto buildDriverContact(Trip trip, Locale locale) {
        ReceiverContactDto dto = new ReceiverContactDto();
        dto.setDestinationName(trip.getFromWarehouseName());
        Long fleetDriverId = trip.getFleetDriverId();
        if (fleetDriverId == null) {
            dto.setReachable(false);
            return dto;
        }
        try {
            FleetDriverFeignResponse response = fleetManagementServiceClient.findFleetDriverById(fleetDriverId, locale);
            if (response != null && response.isSuccess() && response.getFleetDriverDto() != null) {
                FleetDriverSummaryDto driver = response.getFleetDriverDto();
                dto.setUserId(driver.getUserId());
                String name = buildName(driver.getFirstName(), driver.getLastName());
                dto.setName(StringUtils.hasText(name) ? name : "Driver");
                dto.setPhoneNumber(driver.getPhoneNumber());
                dto.setReachable(StringUtils.hasText(driver.getPhoneNumber()));
            }
        } catch (Exception ex) {
            log.warn("Could not resolve driver contact for fleetDriverId={}: {}", fleetDriverId, ex.getMessage());
        }
        return dto;
    }

    private ReceiverContactDto buildReceiverContact(Trip trip, Locale locale) {
        ReceiverContactDto dto = new ReceiverContactDto();
        dto.setDestinationName(trip.getToWarehouseName());
        Long receiverUserId = trip.getReceiverUserId();
        dto.setUserId(receiverUserId);
        if (receiverUserId == null) {
            dto.setReachable(false);
            return dto;
        }
        UserDto user = lookupUser(receiverUserId, locale);
        if (user != null) {
            String name = buildName(user.getFirstName(), user.getLastName());
            dto.setName(StringUtils.hasText(name) ? name : user.getUsername());
            dto.setPhoneNumber(user.getPhoneNumber());
            dto.setEmail(user.getEmail());
            dto.setReachable(StringUtils.hasText(user.getPhoneNumber()) || StringUtils.hasText(user.getEmail()));
        }
        return dto;
    }

    private String resolveSenderName(Participant participant, Trip trip, Locale locale) {
        UserDto user = lookupUser(participant.userId(), locale);
        if (user != null) {
            String name = buildName(user.getFirstName(), user.getLastName());
            if (StringUtils.hasText(name)) {
                return name;
            }
        }
        return participant.role() == TripMessageSenderRole.DRIVER ? "Driver" : "Receiver";
    }

    private UserDto lookupUser(Long userId, Locale locale) {
        try {
            UserResponse response = userManagementServiceClient.findById(userId, locale);
            if (response != null && response.isSuccess() && response.getUserDto() != null) {
                return response.getUserDto();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve user {} for chat: {}", userId, ex.getMessage());
        }
        return null;
    }

    private TripMessageDto toDto(TripMessage m, Long currentUserId) {
        TripMessageDto dto = new TripMessageDto();
        dto.setId(m.getId());
        dto.setSenderUserId(m.getSenderUserId());
        dto.setSenderRole(m.getSenderRole() != null ? m.getSenderRole().name() : null);
        dto.setSenderName(m.getSenderName());
        dto.setBody(m.getBody());
        if (m.getCreatedAt() != null) {
            dto.setCreatedAt(m.getCreatedAt().toString());
            dto.setCreatedAtLabel(m.getCreatedAt().format(STAMP));
        }
        dto.setMine(currentUserId != null && currentUserId.equals(m.getSenderUserId()));
        dto.setRead(m.getReadAt() != null);
        return dto;
    }

    private static String buildName(String first, String last) {
        String f = first != null ? first.trim() : "";
        String l = last != null ? last.trim() : "";
        return (f + " " + l).trim();
    }

    private static TripChatResponse chatError(int status, String message) {
        TripChatResponse response = new TripChatResponse();
        response.setSuccess(false);
        response.setStatusCode(status);
        response.setMessage(message);
        return response;
    }
}
