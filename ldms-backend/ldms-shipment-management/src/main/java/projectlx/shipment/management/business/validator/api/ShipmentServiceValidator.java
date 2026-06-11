package projectlx.shipment.management.business.validator.api;

import projectlx.shipment.management.utils.requests.AllocateShipmentRequest;
import projectlx.shipment.management.utils.requests.ShipmentMultipleFiltersRequest;
import projectlx.shipment.management.utils.requests.UpdateShipmentStatusRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface ShipmentServiceValidator {

    ValidatorDto isAllocateShipmentRequestValid(AllocateShipmentRequest request, Locale locale);

    ValidatorDto isUpdateShipmentStatusRequestValid(UpdateShipmentStatusRequest request, Locale locale);

    ValidatorDto isShipmentMultipleFiltersRequestValid(ShipmentMultipleFiltersRequest request, Locale locale);
}
