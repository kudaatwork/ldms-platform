package projectlx.user.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.logic.api.HelpSupportService;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.SupportTicket;
import projectlx.user.management.repository.SupportTicketRepository;
import projectlx.user.management.service.processor.api.HelpSupportServiceProcessor;
import projectlx.user.management.utils.dtos.SupportTicketDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateSupportTicketRequest;
import projectlx.user.management.utils.responses.HelpSupportResponse;

import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class HelpSupportServiceProcessorImpl implements HelpSupportServiceProcessor {

    private static final Logger logger = LoggerFactory.getLogger(HelpSupportServiceProcessorImpl.class);

    private final HelpSupportService helpSupportService;
    private final SupportTicketRepository supportTicketRepository;
    private final ModelMapper modelMapper;
    private final MessageService messageService;

    @Override
    public HelpSupportResponse listArticles(Locale locale, projectlx.user.management.model.HelpArticleCategory category) {
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
        List<SupportTicket> tickets = supportTicketRepository.findByEntityStatusNotOrderByCreatedAtDesc(EntityStatus.DELETED);
        List<SupportTicketDto> dtos = modelMapper.map(tickets, new TypeToken<List<SupportTicketDto>>() {}.getType());
        String message = messageService.getMessage(I18Code.MESSAGE_SUPPORT_TICKETS_RETRIEVED.getCode(), new String[]{}, locale);
        HelpSupportResponse response = new HelpSupportResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage(message);
        response.setSupportTicketDtoList(dtos);
        return response;
    }
}
