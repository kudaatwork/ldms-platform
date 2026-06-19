package projectlx.co.zw.organizationmanagement.service.rest.frontend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.organizationmanagement.service.processor.api.TradingPartnerServiceProcessor;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateTradingPartnerRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateTradingPartnerRequest;
import projectlx.co.zw.organizationmanagement.utils.responses.OrganizationManagementResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.util.Locale;

@RestController
@RequestMapping("/ldms-organization-management/v1/frontend/organization/trading-partners")
@Tag(name = "Trading Partners (frontend)", description = "CRM-style trading partner directory — customers and suppliers")
@RequiredArgsConstructor
public class TradingPartnerFrontendResource {

    private static final Logger logger = LoggerFactory.getLogger(TradingPartnerFrontendResource.class);

    private final TradingPartnerServiceProcessor tradingPartnerServiceProcessor;

    @Auditable(action = "TRADING_PARTNER_LIST")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).UPDATE_MY_ORGAN.toString())")
    @GetMapping
    @Operation(summary = "List all trading partners for the signed-in organisation")
    public OrganizationManagementResponse list(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return tradingPartnerServiceProcessor.list(locale, username);
    }

    @Auditable(action = "TRADING_PARTNER_CREATE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).UPDATE_MY_ORGAN.toString())")
    @PostMapping
    @Operation(summary = "Create a trading partner for the signed-in organisation")
    public OrganizationManagementResponse create(
            @RequestBody CreateTradingPartnerRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return tradingPartnerServiceProcessor.create(request, locale, username);
    }

    @Auditable(action = "TRADING_PARTNER_UPDATE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).UPDATE_MY_ORGAN.toString())")
    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Update a trading partner belonging to the signed-in organisation")
    public OrganizationManagementResponse update(
            @PathVariable Long id,
            @RequestBody UpdateTradingPartnerRequest request,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return tradingPartnerServiceProcessor.update(id, request, locale, username);
    }

    @Auditable(action = "TRADING_PARTNER_DELETE")
    @PreAuthorize("hasRole(T(projectlx.co.zw.organizationmanagement.utils.security.OrganizationRoles).UPDATE_MY_ORGAN.toString())")
    @DeleteMapping("/{id:\\d+}")
    @Operation(summary = "Soft-delete a trading partner from the signed-in organisation")
    public OrganizationManagementResponse delete(
            @PathVariable Long id,
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE) Locale locale) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return tradingPartnerServiceProcessor.delete(id, locale, username);
    }
}
