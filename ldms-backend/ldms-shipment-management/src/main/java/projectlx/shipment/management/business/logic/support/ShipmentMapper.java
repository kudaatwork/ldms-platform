package projectlx.shipment.management.business.logic.support;

import projectlx.shipment.management.model.Shipment;
import projectlx.shipment.management.utils.dtos.ShipmentDto;

public final class ShipmentMapper {

    private ShipmentMapper() {}

    public static ShipmentDto toDto(Shipment shipment) {
        if (shipment == null) {
            return null;
        }
        ShipmentDto dto = new ShipmentDto();
        dto.setId(shipment.getId());
        dto.setShipmentNumber(shipment.getShipmentNumber());
        dto.setOrganizationId(shipment.getOrganizationId());
        dto.setSourceType(shipment.getSourceType() != null ? shipment.getSourceType().name() : null);
        dto.setInventoryTransferId(shipment.getInventoryTransferId());
        dto.setSalesOrderId(shipment.getSalesOrderId());
        dto.setPurchaseOrderId(shipment.getPurchaseOrderId());
        dto.setCustomerOrganizationId(shipment.getCustomerOrganizationId());
        dto.setFromWarehouseLocationId(shipment.getFromWarehouseLocationId());
        dto.setToWarehouseLocationId(shipment.getToWarehouseLocationId());
        dto.setFromWarehouseName(shipment.getFromWarehouseName());
        dto.setToWarehouseName(shipment.getToWarehouseName());
        dto.setProductId(shipment.getProductId());
        dto.setProductName(shipment.getProductName());
        dto.setProductCode(shipment.getProductCode());
        dto.setQuantity(shipment.getQuantity());
        dto.setCrossBorder(shipment.isCrossBorder());
        dto.setFleetDriverId(shipment.getFleetDriverId());
        dto.setFleetAssetId(shipment.getFleetAssetId());
        dto.setTransportCompanyOrganizationId(shipment.getTransportCompanyOrganizationId());
        dto.setTransportCompanyName(shipment.getTransportCompanyName());
        dto.setTripId(shipment.getTripId());
        dto.setStatus(shipment.getStatus() != null ? shipment.getStatus().name() : null);
        dto.setNotes(shipment.getNotes());
        dto.setEntityStatus(shipment.getEntityStatus() != null ? shipment.getEntityStatus().name() : null);
        dto.setCreatedAt(shipment.getCreatedAt());
        dto.setCreatedBy(shipment.getCreatedBy());
        dto.setModifiedAt(shipment.getModifiedAt());
        dto.setModifiedBy(shipment.getModifiedBy());
        return dto;
    }
}
