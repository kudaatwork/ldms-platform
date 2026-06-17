package projectlx.user.management.business.logic.api;

import projectlx.user.management.model.HelpArticleCategory;
import projectlx.user.management.utils.requests.AddSupportTicketMessageRequest;
import projectlx.user.management.utils.requests.AssignSupportTicketRequest;
import projectlx.user.management.utils.requests.CreateDemoRequisitionRequest;
import projectlx.user.management.utils.requests.CreateSupportTicketRequest;
import projectlx.user.management.utils.requests.SupportTicketExportFilterRequest;
import projectlx.user.management.utils.requests.UpdateDemoRequisitionStatusRequest;
import projectlx.user.management.utils.requests.UpdateSupportTicketStatusRequest;
import projectlx.user.management.utils.responses.HelpSupportResponse;

import java.util.Locale;

public interface HelpSupportService {

    HelpSupportResponse listArticles(Locale locale, HelpArticleCategory category);

    HelpSupportResponse findArticleBySlug(String slug, Locale locale);

    HelpSupportResponse createSupportTicket(CreateSupportTicketRequest request, Locale locale, String username);

    HelpSupportResponse listMySupportTickets(Locale locale, String username);

    HelpSupportResponse platformStatus(Locale locale);

    HelpSupportResponse listAllSupportTickets(Locale locale);

    HelpSupportResponse findSupportTicketById(Long id, Locale locale, boolean includeInternalMessages);

    HelpSupportResponse findMySupportTicketById(Long id, Locale locale, String username);

    HelpSupportResponse updateSupportTicketStatus(UpdateSupportTicketStatusRequest request, Locale locale, String actorUsername);

    HelpSupportResponse assignSupportTicket(AssignSupportTicketRequest request, Locale locale, String actorUsername);

    HelpSupportResponse addSupportTicketMessage(AddSupportTicketMessageRequest request, Locale locale, String actorUsername, boolean handlerActor);

    byte[] exportSupportTickets(SupportTicketExportFilterRequest filters, String format, Locale locale) throws Exception;

    HelpSupportResponse submitDemoRequisition(CreateDemoRequisitionRequest request, Locale locale);

    HelpSupportResponse listAllDemoRequisitions(Locale locale);

    HelpSupportResponse findDemoRequisitionById(Long id, Locale locale);

    HelpSupportResponse updateDemoRequisitionStatus(UpdateDemoRequisitionStatusRequest request,
                                                    Locale locale,
                                                    String actorUsername);
}
