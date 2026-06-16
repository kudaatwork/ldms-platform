package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.inventory.management.clients.ShipmentManagementServiceClient;
import projectlx.inventory.management.clients.dto.ShipmentFeignResponse;
import projectlx.inventory.management.clients.dto.ShipmentSummaryDto;
import projectlx.inventory.management.utils.enums.I18Code;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesOrderDispatchSupport {

    private static final String STATUS_ALLOCATED = "ALLOCATED";
    private static final String STATUS_ARRIVED_PENDING_OTP = "ARRIVED_PENDING_OTP";

    private final ShipmentManagementServiceClient shipmentManagementServiceClient;
    private final MessageService messageService;

    public Optional<ShipmentSummaryDto> findShipmentForSalesOrder(Long salesOrderId, Locale locale) {
        if (salesOrderId == null || salesOrderId <= 0) {
            return Optional.empty();
        }
        try {
            ShipmentFeignResponse response = shipmentManagementServiceClient.findBySalesOrderId(salesOrderId, locale);
            if (response != null && response.isSuccess() && response.getShipmentDto() != null) {
                return Optional.of(response.getShipmentDto());
            }
        } catch (Exception ex) {
            log.warn("Could not resolve shipment for sales order {}: {}", salesOrderId, ex.getMessage());
        }
        return Optional.empty();
    }

    public Optional<String> validateDispatchReadyForTransit(Long salesOrderId, Long shipmentId, Locale locale) {
        Optional<ShipmentSummaryDto> shipmentOpt = findShipmentForSalesOrder(salesOrderId, locale);
        if (shipmentOpt.isEmpty()) {
            return Optional.of(messageService.getMessage(
                    I18Code.MESSAGE_SALES_ORDER_DISPATCH_NOT_READY.getCode(), new String[]{}, locale));
        }

        ShipmentSummaryDto shipment = shipmentOpt.get();
        if (shipmentId != null && shipment.getId() != null && !shipmentId.equals(shipment.getId())) {
            return Optional.of(messageService.getMessage(
                    I18Code.MESSAGE_SALES_ORDER_DISPATCH_NOT_READY.getCode(), new String[]{}, locale));
        }
        if (!STATUS_ALLOCATED.equalsIgnoreCase(shipment.getStatus())) {
            return Optional.of(messageService.getMessage(
                    I18Code.MESSAGE_SALES_ORDER_DISPATCH_NOT_READY.getCode(), new String[]{}, locale));
        }
        if (shipment.getFleetDriverId() == null || shipment.getFleetAssetId() == null) {
            return Optional.of(messageService.getMessage(
                    I18Code.MESSAGE_SALES_ORDER_DISPATCH_NOT_READY.getCode(), new String[]{}, locale));
        }
        return Optional.empty();
    }

    public Optional<String> validateReceiverAcknowledgmentReady(Long salesOrderId, Locale locale) {
        Optional<ShipmentSummaryDto> shipmentOpt = findShipmentForSalesOrder(salesOrderId, locale);
        if (shipmentOpt.isEmpty()) {
            return Optional.of(messageService.getMessage(
                    I18Code.MESSAGE_SALES_ORDER_COMPLETE_REQUIRES_RECEIVER_ACK.getCode(),
                    new String[]{}, locale));
        }
        if (!STATUS_ARRIVED_PENDING_OTP.equalsIgnoreCase(shipmentOpt.get().getStatus())) {
            return Optional.of(messageService.getMessage(
                    I18Code.MESSAGE_SALES_ORDER_COMPLETE_REQUIRES_RECEIVER_ACK.getCode(),
                    new String[]{}, locale));
        }
        return Optional.empty();
    }

    public boolean hasLinkedShipment(Long salesOrderId, Locale locale) {
        return findShipmentForSalesOrder(salesOrderId, locale).isPresent();
    }
}
