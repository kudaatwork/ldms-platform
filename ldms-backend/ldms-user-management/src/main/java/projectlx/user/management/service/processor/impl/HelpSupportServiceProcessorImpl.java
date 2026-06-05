package projectlx.user.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.user.management.business.logic.api.HelpSupportService;
import projectlx.user.management.model.HelpArticleCategory;
import projectlx.user.management.service.processor.api.HelpSupportServiceProcessor;
import projectlx.user.management.utils.requests.AddSupportTicketMessageRequest;
import projectlx.user.management.utils.requests.AssignSupportTicketRequest;
import projectlx.user.management.utils.requests.CreateSupportTicketRequest;
import projectlx.user.management.utils.requests.SupportTicketExportFilterRequest;
import projectlx.user.management.utils.requests.UpdateSupportTicketStatusRequest;
import projectlx.user.management.utils.responses.HelpSupportResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class HelpSupportServiceProcessorImpl implements HelpSupportServiceProcessor {

    private static final Logger logger = LoggerFactory.getLogger(HelpSupportServiceProcessorImpl.class);

    private final HelpSupportService helpSupportService;

    @Override
    public HelpSupportResponse listArticles(Locale locale, HelpArticleCategory category) {
        return helpSupportService.listArticles(locale, category);
    }

    @Override
    public HelpSupportResponse findArticleBySlug(String slug, Locale locale) {
        return helpSupportService.findArticleBySlug(slug, locale);
    }

    @Override
    public HelpSupportResponse createSupportTicket(CreateSupportTicketRequest request, Locale locale, String username) {
        logger.info("Support ticket create requested by {}", username);
        return helpSupportService.createSupportTicket(request, locale, username);
    }

    @Override
    public HelpSupportResponse listMySupportTickets(Locale locale, String username) {
        return helpSupportService.listMySupportTickets(locale, username);
    }

    @Override
    public HelpSupportResponse platformStatus(Locale locale) {
        return helpSupportService.platformStatus(locale);
    }

    @Override
    public HelpSupportResponse listAllSupportTickets(Locale locale) {
        return helpSupportService.listAllSupportTickets(locale);
    }

    @Override
    public HelpSupportResponse findSupportTicketById(Long id, Locale locale, boolean includeInternalMessages) {
        return helpSupportService.findSupportTicketById(id, locale, includeInternalMessages);
    }

    @Override
    public HelpSupportResponse findMySupportTicketById(Long id, Locale locale, String username) {
        return helpSupportService.findMySupportTicketById(id, locale, username);
    }

    @Override
    public HelpSupportResponse updateSupportTicketStatus(UpdateSupportTicketStatusRequest request,
                                                           Locale locale,
                                                           String actorUsername) {
        logger.info("Support ticket {} status update requested by {}", request.getSupportTicketId(), actorUsername);
        return helpSupportService.updateSupportTicketStatus(request, locale, actorUsername);
    }

    @Override
    public HelpSupportResponse assignSupportTicket(AssignSupportTicketRequest request,
                                                   Locale locale,
                                                   String actorUsername) {
        logger.info("Support ticket {} assignment requested by {}", request.getSupportTicketId(), actorUsername);
        return helpSupportService.assignSupportTicket(request, locale, actorUsername);
    }

    @Override
    public HelpSupportResponse addSupportTicketMessage(AddSupportTicketMessageRequest request,
                                                       Locale locale,
                                                       String actorUsername,
                                                       boolean handlerActor) {
        logger.info("Support ticket {} message requested by {}", request.getSupportTicketId(), actorUsername);
        return helpSupportService.addSupportTicketMessage(request, locale, actorUsername, handlerActor);
    }

    @Override
    public byte[] exportSupportTickets(SupportTicketExportFilterRequest filters, String format, Locale locale) throws Exception {
        return helpSupportService.exportSupportTickets(filters, format, locale);
    }
}
