package projectlx.shipment.management.business.logic.impl;

import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.shipment.management.business.logic.api.PlatformDashboardService;
import projectlx.shipment.management.business.logic.support.PlatformDashboardSupport;
import projectlx.shipment.management.utils.dtos.PlatformShipmentDashboardDto;
import projectlx.shipment.management.utils.enums.I18Code;
import projectlx.shipment.management.utils.responses.PlatformShipmentDashboardResponse;

import java.util.List;
import java.util.Locale;

public class PlatformDashboardServiceImpl implements PlatformDashboardService {

    private final PlatformDashboardSupport platformDashboardSupport;
    private final MessageService messageService;

    public PlatformDashboardServiceImpl(PlatformDashboardSupport platformDashboardSupport,
                                       MessageService messageService) {
        this.platformDashboardSupport = platformDashboardSupport;
        this.messageService = messageService;
    }

    @Override
    public PlatformShipmentDashboardResponse getShipmentDashboard(Locale locale) {
        PlatformShipmentDashboardDto dashboard = platformDashboardSupport.buildDashboardSnapshot();
        PlatformShipmentDashboardResponse response = new PlatformShipmentDashboardResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(
                I18Code.MESSAGE_SHIPMENT_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setPlatformShipmentDashboardDto(dashboard);
        return response;
    }

    @Override
    public PlatformShipmentDashboardResponse searchShipments(String term, List<Long> purchaseOrderIds, int limit,
                                                             Locale locale) {
        PlatformShipmentDashboardDto dashboard = new PlatformShipmentDashboardDto();
        dashboard.setLiveShipments(platformDashboardSupport.searchShipments(term, purchaseOrderIds, limit));
        PlatformShipmentDashboardResponse response = new PlatformShipmentDashboardResponse();
        response.setSuccess(true);
        response.setStatusCode(200);
        response.setMessage(messageService.getMessage(
                I18Code.MESSAGE_SHIPMENT_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setPlatformShipmentDashboardDto(dashboard);
        return response;
    }
}
