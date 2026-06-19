package projectlx.co.zw.organizationmanagement.business.logic.api;

import projectlx.co.zw.organizationmanagement.utils.requests.CreateTradingPartnerRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateTradingPartnerRequest;
import projectlx.co.zw.organizationmanagement.utils.responses.OrganizationManagementResponse;

import java.util.Locale;

public interface TradingPartnerService {

    OrganizationManagementResponse list(Locale locale, String username);

    OrganizationManagementResponse create(CreateTradingPartnerRequest request, Locale locale, String username);

    OrganizationManagementResponse update(Long id, UpdateTradingPartnerRequest request, Locale locale, String username);

    OrganizationManagementResponse delete(Long id, Locale locale, String username);
}
