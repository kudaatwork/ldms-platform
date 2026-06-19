package projectlx.co.zw.organizationmanagement.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.organizationmanagement.business.logic.api.TradingPartnerService;
import projectlx.co.zw.organizationmanagement.service.processor.api.TradingPartnerServiceProcessor;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateTradingPartnerRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateTradingPartnerRequest;
import projectlx.co.zw.organizationmanagement.utils.responses.OrganizationManagementResponse;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class TradingPartnerServiceProcessorImpl implements TradingPartnerServiceProcessor {

    private final TradingPartnerService tradingPartnerService;

    @Override
    public OrganizationManagementResponse list(Locale locale, String username) {
        log.info("Incoming request: listTradingPartners user={}", username);
        OrganizationManagementResponse response = tradingPartnerService.list(locale, username);
        log.info("Outgoing response: listTradingPartners success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationManagementResponse create(CreateTradingPartnerRequest request, Locale locale, String username) {
        log.info("Incoming request: createTradingPartner role={} user={}",
                request != null ? request.getPartnerRole() : null, username);
        OrganizationManagementResponse response = tradingPartnerService.create(request, locale, username);
        log.info("Outgoing response: createTradingPartner success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationManagementResponse update(Long id, UpdateTradingPartnerRequest request, Locale locale, String username) {
        log.info("Incoming request: updateTradingPartner id={} user={}", id, username);
        OrganizationManagementResponse response = tradingPartnerService.update(id, request, locale, username);
        log.info("Outgoing response: updateTradingPartner success={}", response.isSuccess());
        return response;
    }

    @Override
    public OrganizationManagementResponse delete(Long id, Locale locale, String username) {
        log.info("Incoming request: deleteTradingPartner id={} user={}", id, username);
        OrganizationManagementResponse response = tradingPartnerService.delete(id, locale, username);
        log.info("Outgoing response: deleteTradingPartner success={}", response.isSuccess());
        return response;
    }
}
