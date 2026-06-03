package projectlx.user.management.business.logic.impl;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.logic.api.HelpSupportService;
import projectlx.user.management.business.logic.api.PlatformHealthService;
import projectlx.user.management.business.validator.api.HelpSupportServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.HelpArticle;
import projectlx.user.management.model.HelpArticleCategory;
import projectlx.user.management.model.SupportTicket;
import projectlx.user.management.model.SupportTicketPriority;
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
import projectlx.user.management.utils.responses.HelpSupportResponse;
import projectlx.user.management.utils.responses.PlatformHealthResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class HelpSupportServiceImpl implements HelpSupportService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final HelpSupportServiceValidator helpSupportServiceValidator;
    private final MessageService messageService;
    private final HelpArticleRepository helpArticleRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final UserRepository userRepository;
    private final PlatformHealthService platformHealthService;
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
        SupportTicket ticket = new SupportTicket();
        ticket.setSubject(request.getSubject().trim());
        ticket.setDescription(request.getDescription().trim());
        ticket.setCategory(request.getCategory());
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : SupportTicketPriority.NORMAL);
        ticket.setRequesterUsername(username);
        ticket.setRequesterEmail(user.getEmail());
        ticket.setOrganizationId(user.getOrganizationId());
        ticket.setCreatedBy(username);
        ticket.setModifiedBy(username);

        SupportTicket saved = supportTicketRepository.save(ticket);
        saved.setTicketNumber(formatTicketNumber(saved.getId()));
        saved = supportTicketRepository.save(saved);

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

        String message = messageService.getMessage(I18Code.MESSAGE_HELP_PLATFORM_STATUS_RETRIEVED.getCode(), new String[]{}, locale);
        HelpSupportResponse response = success(200, message, null, null, null, null);
        response.setPlatformStatusDto(statusDto);
        return response;
    }

    private String formatTicketNumber(Long id) {
        int year = LocalDateTime.now().getYear();
        return "LX-" + year + "-" + String.format("%06d", id);
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
