package projectlx.user.management.business.logic.api;

import projectlx.user.management.model.HelpArticleCategory;
import projectlx.user.management.utils.requests.CreateSupportTicketRequest;
import projectlx.user.management.utils.responses.HelpSupportResponse;

import java.util.Locale;

public interface HelpSupportService {

    HelpSupportResponse listArticles(Locale locale, HelpArticleCategory category);

    HelpSupportResponse findArticleBySlug(String slug, Locale locale);

    HelpSupportResponse createSupportTicket(CreateSupportTicketRequest request, Locale locale, String username);

    HelpSupportResponse listMySupportTickets(Locale locale, String username);

    HelpSupportResponse platformStatus(Locale locale);
}
