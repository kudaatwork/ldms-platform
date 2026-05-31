package projectlx.user.management.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.model.HelpArticleCategory;
import projectlx.user.management.service.processor.api.HelpSupportServiceProcessor;
import projectlx.user.management.utils.requests.CreateSupportTicketRequest;
import projectlx.user.management.utils.responses.HelpSupportResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/frontend/help-support")
@Tag(name = "Help & Support Frontend Resource", description = "FAQ articles, support tickets, and platform status for portal users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class HelpSupportFrontendResource {

    private final HelpSupportServiceProcessor helpSupportServiceProcessor;

    @Auditable(action = "LIST_HELP_ARTICLES")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/articles")
    @Operation(summary = "List help articles", description = "Returns active FAQ articles, optionally filtered by category.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Articles retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public HelpSupportResponse listArticles(
            @RequestParam(value = "category", required = false) HelpArticleCategory category,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return helpSupportServiceProcessor.listArticles(locale, category);
    }

    @Auditable(action = "FIND_HELP_ARTICLE_BY_SLUG")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/articles/{slug}")
    @Operation(summary = "Get help article by slug")
    public HelpSupportResponse findArticleBySlug(
            @PathVariable("slug") String slug,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return helpSupportServiceProcessor.findArticleBySlug(slug, locale);
    }

    @Auditable(action = "CREATE_SUPPORT_TICKET")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/support-ticket/create")
    @Operation(summary = "Create a support ticket", description = "Opens a new support ticket for the signed-in user.")
    public HelpSupportResponse createSupportTicket(
            @Valid @RequestBody CreateSupportTicketRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return helpSupportServiceProcessor.createSupportTicket(request, locale, username);
    }

    @Auditable(action = "LIST_MY_SUPPORT_TICKETS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/support-ticket/my-tickets")
    @Operation(summary = "List my support tickets")
    public HelpSupportResponse listMySupportTickets(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return helpSupportServiceProcessor.listMySupportTickets(locale, username);
    }

    @Auditable(action = "HELP_PLATFORM_STATUS")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/platform-status")
    @Operation(summary = "Platform status summary", description = "User-friendly operational status snapshot.")
    public HelpSupportResponse platformStatus(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) final Locale locale) {
        return helpSupportServiceProcessor.platformStatus(locale);
    }
}
