package projectlx.user.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.logic.api.HelpSupportService;
import projectlx.user.management.business.logic.api.PlatformHealthService;
import projectlx.user.management.business.logic.support.SupportTicketAssignmentService;
import projectlx.user.management.business.logic.support.SupportTicketOperationsSupport;
import projectlx.user.management.business.logic.support.SupportTicketWorkflowSupport;
import projectlx.user.management.business.validator.api.HelpSupportServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.HelpArticle;
import projectlx.user.management.model.HelpArticleCategory;
import projectlx.user.management.model.SupportTicket;
import projectlx.user.management.model.SupportTicketMessageAuthorRole;
import projectlx.user.management.model.SupportTicketMessageVisibility;
import projectlx.user.management.model.SupportTicketPriority;
import projectlx.user.management.model.SupportTicketStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.repository.HelpArticleRepository;
import projectlx.user.management.repository.SupportTicketRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.utils.dtos.HelpArticleDto;
import projectlx.user.management.utils.dtos.HelpPlatformStatusDto;
import projectlx.user.management.utils.dtos.PlatformHealthSummaryDto;
import projectlx.user.management.utils.dtos.SupportTicketDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.enums.PlatformOverallStatus;
import projectlx.user.management.utils.requests.CreateSupportTicketRequest;
import projectlx.user.management.utils.requests.AddSupportTicketMessageRequest;
import projectlx.user.management.utils.requests.AssignSupportTicketRequest;
import projectlx.user.management.utils.requests.SupportTicketExportFilterRequest;
import projectlx.user.management.utils.requests.UpdateSupportTicketStatusRequest;
import projectlx.user.management.utils.responses.HelpSupportResponse;
import projectlx.user.management.utils.responses.PlatformHealthResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class HelpSupportServiceImpl implements HelpSupportService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    /** Matches {@code support_ticket.ticket_number} VARCHAR(32). */
    private static final int TICKET_NUMBER_MAX_LENGTH = 32;
    /** Platform health probes every registered service; cache briefly for Help & Support page loads. */
    private static final long PLATFORM_STATUS_CACHE_MS = 90_000L;

    private volatile HelpPlatformStatusDto cachedPlatformStatus;
    private volatile long cachedPlatformStatusAtMs;

    private final HelpSupportServiceValidator helpSupportServiceValidator;
    private final MessageService messageService;
    private final HelpArticleRepository helpArticleRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;
    private final PlatformHealthService platformHealthService;
    private final SupportTicketAssignmentService supportTicketAssignmentService;
    private final SupportTicketOperationsSupport ticketOperations;
    private final ModelMapper modelMapper;

    @Override
    public HelpSupportResponse listArticles(Locale locale, HelpArticleCategory category) {
        List<HelpArticle> articles = category == null
                ? helpArticleRepository.findByEntityStatusOrderBySortOrderAscTitleAsc(EntityStatus.ACTIVE)
                : helpArticleRepository.findByCategoryAndEntityStatusOrderBySortOrderAscTitleAsc(
                        category, EntityStatus.ACTIVE);

        List<HelpArticleDto> dtos = modelMapper.map(articles, new TypeToken<List<HelpArticleDto>>() {}.getType());
        String message = messageService.getMessage(I18Code.MESSAGE_HELP_ARTICLES_RETRIEVED.getCode(), new String[]{}, locale);
        return success(200, message, dtos, null, null, null);
    }

    @Override
    public HelpSupportResponse findArticleBySlug(String slug, Locale locale) {
        Optional<HelpArticle> article = helpArticleRepository.findBySlugAndEntityStatus(slug, EntityStatus.ACTIVE);
        if (article.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_HELP_ARTICLE_NOT_FOUND.getCode(), new String[]{}, locale);
            return failure(404, message);
        }
        HelpArticleDto dto = modelMapper.map(article.get(), HelpArticleDto.class);
        String message = messageService.getMessage(I18Code.MESSAGE_HELP_ARTICLE_RETRIEVED.getCode(), new String[]{}, locale);
        HelpSupportResponse response = success(200, message, null, dto, null, null);
        response.setHelpArticleDto(dto);
        return response;
    }

    @Override
    public HelpSupportResponse createSupportTicket(CreateSupportTicketRequest request, Locale locale, String username) {
        ValidatorDto validatorDto = helpSupportServiceValidator.isCreateSupportTicketRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_SUPPORT_TICKET_INVALID_REQUEST.getCode(), new String[]{}, locale);
            HelpSupportResponse response = failure(400, message);
            response.setErrorMessages(validatorDto.getErrorMessages());
            return response;
        }

        Optional<User> userOpt = userRepository.findByUsernameAndEntityStatusNot(username, EntityStatus.DELETED);
        if (userOpt.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return failure(404, message);
        }

        User user = userOpt.get();
        String requesterEmail = user.getEmail();
        if (requesterEmail == null || requesterEmail.isBlank()) {
            requesterEmail = username.trim() + "@ldms.local";
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setSubject(request.getSubject().trim());
        ticket.setDescription(request.getDescription().trim());
        ticket.setCategory(request.getCategory());
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : SupportTicketPriority.NORMAL);
        ticket.setRequesterUsername(username);
        ticket.setRequesterEmail(requesterEmail.trim());
        ticket.setOrganizationId(user.getOrganizationId());
        ticket.setCreatedBy(username);
        ticket.setModifiedBy(username);
        // ticket_number is NOT NULL + UNIQUE (VARCHAR 32) — provisional value before ID is known.
        ticket.setTicketNumber(provisionalTicketNumber());

        SupportTicket saved = supportTicketRepository.save(ticket);
        saved.setTicketNumber(formatTicketNumber(saved.getId()));
        saved = supportTicketRepository.save(saved);

        try {
            Optional<User> handler = supportTicketAssignmentService.pickHandler();
            if (handler.isPresent()) {
                User assigned = handler.get();
                saved.setAssignedHandlerUserId(assigned.getId());
                saved.setAssignedHandlerUsername(assigned.getUsername());
                saved = supportTicketRepository.save(saved);
            }
        } catch (Exception ex) {
            log.warn("Support ticket {} saved but handler assignment failed; leaving unassigned", saved.getId(), ex);
        }

        SupportTicketDto dto = modelMapper.map(saved, SupportTicketDto.class);
        String message = messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_CREATED.getCode(), new String[]{}, locale);
        HelpSupportResponse response = success(201, message, null, null, dto, null);
        response.setSupportTicketDto(dto);
        return response;
    }

    @Override
    public HelpSupportResponse listMySupportTickets(Locale locale, String username) {
        List<SupportTicket> tickets = supportTicketRepository
                .findByRequesterUsernameAndEntityStatusNotOrderByCreatedAtDesc(username, EntityStatus.DELETED);
        List<SupportTicketDto> dtos = modelMapper.map(tickets, new TypeToken<List<SupportTicketDto>>() {}.getType());
        String message = messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKETS_RETRIEVED.getCode(), new String[]{}, locale);
        HelpSupportResponse response = success(200, message, null, null, null, dtos);
        response.setSupportTicketDtoList(dtos);
        return response;
    }

    @Override
    public HelpSupportResponse platformStatus(Locale locale) {
        long now = System.currentTimeMillis();
        HelpPlatformStatusDto cached = cachedPlatformStatus;
        if (cached != null && now - cachedPlatformStatusAtMs < PLATFORM_STATUS_CACHE_MS) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_HELP_PLATFORM_STATUS_RETRIEVED.getCode(), new String[]{}, locale);
            HelpSupportResponse response = success(200, message, null, null, null, null);
            response.setPlatformStatusDto(cached);
            return response;
        }

        PlatformHealthResponse health = platformHealthService.snapshot(locale);
        HelpPlatformStatusDto statusDto = new HelpPlatformStatusDto();
        statusDto.setCheckedAt(health.getCheckedAt() != null ? health.getCheckedAt().format(ISO) : LocalDateTime.now().format(ISO));

        PlatformOverallStatus overall = health.getOverallStatus() != null
                ? health.getOverallStatus()
                : PlatformOverallStatus.DEGRADED;
        statusDto.setOverallStatus(overall.name());

        PlatformHealthSummaryDto summary = health.getSummary();
        if (summary != null) {
            statusDto.setTotalServices(summary.getTotalServices());
            statusDto.setUpCount(summary.getUpCount());
            statusDto.setDownCount(summary.getDownCount());
        }

        switch (overall) {
            case OPERATIONAL -> {
                statusDto.setHeadline("All systems operational");
                statusDto.setDetail("Core LDMS services are healthy. If you still experience issues, submit a support ticket.");
            }
            case DEGRADED -> {
                statusDto.setHeadline("Partial degradation");
                statusDto.setDetail("Some services may be slow or unavailable. Workflows may continue with delays.");
            }
            default -> {
                statusDto.setHeadline("Service disruption");
                statusDto.setDetail("One or more critical services are down. Our team is working on restoration.");
            }
        }

        cachedPlatformStatus = statusDto;
        cachedPlatformStatusAtMs = System.currentTimeMillis();

        String message = messageService.getMessage(I18Code.MESSAGE_HELP_PLATFORM_STATUS_RETRIEVED.getCode(), new String[]{}, locale);
        HelpSupportResponse response = success(200, message, null, null, null, null);
        response.setPlatformStatusDto(statusDto);
        return response;
    }

    @Override
    public HelpSupportResponse listAllSupportTickets(Locale locale) {
        List<SupportTicket> tickets = supportTicketRepository.findByEntityStatusNotOrderByCreatedAtDesc(EntityStatus.DELETED);
        List<SupportTicketDto> dtos = ticketOperations.toDtoList(tickets);
        String message = messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKETS_RETRIEVED.getCode(), new String[]{}, locale);
        HelpSupportResponse response = success(200, message, null, null, null, dtos);
        response.setSupportTicketDtoList(dtos);
        return response;
    }

    @Override
    public HelpSupportResponse findSupportTicketById(Long id, Locale locale, boolean includeInternalMessages) {
        Optional<SupportTicket> ticketOpt = ticketOperations.findActiveTicket(id);
        if (ticketOpt.isEmpty()) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        SupportTicketDto dto = ticketOperations.toDto(ticketOpt.get(), includeInternalMessages);
        String message = messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_RETRIEVED.getCode(), new String[]{}, locale);
        HelpSupportResponse response = success(200, message, null, null, dto, null);
        response.setSupportTicketDto(dto);
        return response;
    }

    @Override
    public HelpSupportResponse findMySupportTicketById(Long id, Locale locale, String username) {
        Optional<SupportTicket> ticketOpt = ticketOperations.findActiveTicketForRequester(id, username);
        if (ticketOpt.isEmpty()) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        SupportTicketDto dto = ticketOperations.toDto(ticketOpt.get(), false);
        String message = messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_RETRIEVED.getCode(), new String[]{}, locale);
        HelpSupportResponse response = success(200, message, null, null, dto, null);
        response.setSupportTicketDto(dto);
        return response;
    }

    @Override
    public HelpSupportResponse updateSupportTicketStatus(UpdateSupportTicketStatusRequest request,
                                                           Locale locale,
                                                           String actorUsername) {
        ValidatorDto validatorDto = helpSupportServiceValidator.isUpdateSupportTicketStatusRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            HelpSupportResponse response = failure(400,
                    messageService.getMessage(I18Code.MESSAGE_UPDATE_SUPPORT_TICKET_INVALID_REQUEST.getCode(), new String[]{}, locale));
            response.setErrorMessages(validatorDto.getErrorMessages());
            return response;
        }
        Optional<SupportTicket> ticketOpt = ticketOperations.findActiveTicket(request.getSupportTicketId());
        if (ticketOpt.isEmpty()) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        SupportTicket ticket = ticketOpt.get();
        if (!SupportTicketWorkflowSupport.canTransition(ticket.getStatus(), request.getStatus())) {
            return failure(409, messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_STATUS_INVALID.getCode(), new String[]{}, locale));
        }
        if (request.getStatus() == SupportTicketStatus.IN_PROGRESS
                && ticket.getAssignedHandlerUserId() == null) {
            ticketOperations.resolveHandler(null, actorUsername).ifPresent(handler ->
                    ticketOperations.assignHandler(ticket, handler, actorUsername));
        }
        ticketOperations.applyStatusTransition(ticket, request.getStatus(), actorUsername);
        SupportTicket refreshed = ticketOperations.findActiveTicket(ticket.getId()).orElse(ticket);
        SupportTicketDto dto = ticketOperations.toDto(refreshed, true);
        HelpSupportResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_UPDATED.getCode(), new String[]{}, locale),
                null, null, dto, null);
        response.setSupportTicketDto(dto);
        return response;
    }

    @Override
    public HelpSupportResponse assignSupportTicket(AssignSupportTicketRequest request,
                                                   Locale locale,
                                                   String actorUsername) {
        ValidatorDto validatorDto = helpSupportServiceValidator.isAssignSupportTicketRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            HelpSupportResponse response = failure(400,
                    messageService.getMessage(I18Code.MESSAGE_ASSIGN_SUPPORT_TICKET_INVALID_REQUEST.getCode(), new String[]{}, locale));
            response.setErrorMessages(validatorDto.getErrorMessages());
            return response;
        }
        Optional<SupportTicket> ticketOpt = ticketOperations.findActiveTicket(request.getSupportTicketId());
        if (ticketOpt.isEmpty()) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        Optional<User> handlerOpt = ticketOperations.resolveHandler(request.getHandlerUserId(), actorUsername);
        if (handlerOpt.isEmpty()) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        SupportTicket ticket = ticketOpt.get();
        ticketOperations.assignHandler(ticket, handlerOpt.get(), actorUsername);
        if (ticket.getStatus() == SupportTicketStatus.OPEN) {
            ticketOperations.applyStatusTransition(ticket, SupportTicketStatus.IN_PROGRESS, actorUsername);
        }
        SupportTicket refreshed = ticketOperations.findActiveTicket(ticket.getId()).orElse(ticket);
        SupportTicketDto dto = ticketOperations.toDto(refreshed, true);
        HelpSupportResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_ASSIGNED.getCode(), new String[]{}, locale),
                null, null, dto, null);
        response.setSupportTicketDto(dto);
        return response;
    }

    @Override
    public HelpSupportResponse addSupportTicketMessage(AddSupportTicketMessageRequest request,
                                                       Locale locale,
                                                       String actorUsername,
                                                       boolean handlerActor) {
        ValidatorDto validatorDto = helpSupportServiceValidator.isAddSupportTicketMessageRequestValid(request, locale);
        if (!validatorDto.getSuccess()) {
            HelpSupportResponse response = failure(400,
                    messageService.getMessage(I18Code.MESSAGE_ADD_SUPPORT_TICKET_MESSAGE_INVALID.getCode(), new String[]{}, locale));
            response.setErrorMessages(validatorDto.getErrorMessages());
            return response;
        }
        Optional<SupportTicket> ticketOpt = handlerActor
                ? ticketOperations.findActiveTicket(request.getSupportTicketId())
                : ticketOperations.findActiveTicketForRequester(request.getSupportTicketId(), actorUsername);
        if (ticketOpt.isEmpty()) {
            return failure(404, messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_NOT_FOUND.getCode(), new String[]{}, locale));
        }
        SupportTicket ticket = ticketOpt.get();
        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            return failure(409, messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_CLOSED.getCode(), new String[]{}, locale));
        }
        SupportTicketMessageVisibility visibility = handlerActor
                ? (request.getVisibility() != null ? request.getVisibility() : SupportTicketMessageVisibility.PUBLIC)
                : SupportTicketMessageVisibility.PUBLIC;
        if (!handlerActor && visibility == SupportTicketMessageVisibility.INTERNAL) {
            visibility = SupportTicketMessageVisibility.PUBLIC;
        }
        SupportTicketMessageAuthorRole role = handlerActor
                ? SupportTicketMessageAuthorRole.HANDLER
                : SupportTicketMessageAuthorRole.REQUESTER;
        ticketOperations.addMessage(ticket, actorUsername, role, visibility, request.getBody());
        ticket.setModifiedBy(actorUsername);
        if (!handlerActor && ticket.getStatus() == SupportTicketStatus.WAITING_ON_CUSTOMER) {
            ticketOperations.applyStatusTransition(ticket, SupportTicketStatus.IN_PROGRESS, actorUsername);
        } else {
            supportTicketRepository.save(ticket);
        }
        SupportTicket refreshed = ticketOperations.findActiveTicket(ticket.getId()).orElse(ticket);
        SupportTicketDto dto = ticketOperations.toDto(refreshed, handlerActor);
        HelpSupportResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKET_MESSAGE_ADDED.getCode(), new String[]{}, locale),
                null, null, dto, null);
        response.setSupportTicketDto(dto);
        return response;
    }

    @Override
    public byte[] exportSupportTickets(SupportTicketExportFilterRequest filters, String format, Locale locale) throws Exception {
        List<SupportTicket> tickets = ticketOperations.filterForExport(filters);
        String normalized = format == null ? "csv" : format.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "csv" -> ticketOperations.exportCsv(tickets);
            case "excel", "xlsx" -> ticketOperations.exportExcel(tickets);
            case "pdf" -> ticketOperations.exportPdf(tickets);
            default -> throw new IllegalArgumentException("Unsupported export format: " + format);
        };
    }

    private String formatTicketNumber(Long id) {
        int year = LocalDateTime.now().getYear();
        return "LX-" + year + "-" + String.format("%06d", id);
    }

    /**
     * Unique placeholder that fits {@code ticket_number} VARCHAR(32) before the row id is assigned.
     */
    private String provisionalTicketNumber() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String provisional = "LX-P-" + suffix;
        if (provisional.length() > TICKET_NUMBER_MAX_LENGTH) {
            return provisional.substring(0, TICKET_NUMBER_MAX_LENGTH);
        }
        return provisional;
    }

    private HelpSupportResponse success(int statusCode, String message,
                                        List<HelpArticleDto> articles,
                                        HelpArticleDto article,
                                        SupportTicketDto ticket,
                                        List<SupportTicketDto> tickets) {
        HelpSupportResponse response = new HelpSupportResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        response.setHelpArticleDtoList(articles);
        response.setHelpArticleDto(article);
        response.setSupportTicketDto(ticket);
        response.setSupportTicketDtoList(tickets);
        return response;
    }

    private HelpSupportResponse failure(int statusCode, String message) {
        HelpSupportResponse response = new HelpSupportResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
