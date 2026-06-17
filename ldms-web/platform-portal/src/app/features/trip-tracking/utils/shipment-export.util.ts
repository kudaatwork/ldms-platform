import type { BorderClearanceCaseRow } from '../services/border-clearance-portal.service';
import type { ShipmentRow, TripRow } from '../models/trip-tracking.model';
import type { LxExportColumn } from '../../../shared/utils/lx-export.util';

export const SHIPMENT_EXPORT_COLUMNS: LxExportColumn<ShipmentRow>[] = [
  { header: 'SHIPMENT_NUMBER', value: (r) => r.shipmentNumber },
  { header: 'TRANSFER_REF', value: (r) => r.transferReference },
  { header: 'FROM_WAREHOUSE', value: (r) => r.fromWarehouse },
  { header: 'TO_WAREHOUSE', value: (r) => r.toWarehouse },
  { header: 'PRODUCT', value: (r) => r.productName },
  { header: 'QUANTITY', value: (r) => r.quantity },
  { header: 'UNIT_OF_MEASURE', value: (r) => r.unitOfMeasure },
  { header: 'STATUS', value: (r) => r.statusLabel },
  { header: 'TRANSPORT_COMPANY', value: (r) => r.transportCompanyName ?? '' },
  { header: 'DRIVER', value: (r) => r.driverName },
  { header: 'VEHICLE', value: (r) => r.vehicleRegistration },
  { header: 'CREATED', value: (r) => r.createdAtLabel },
];

export const TRIP_EXPORT_COLUMNS: LxExportColumn<TripRow>[] = [
  { header: 'TRIP_NUMBER', value: (r) => r.tripNumber },
  { header: 'SHIPMENT_NUMBER', value: (r) => r.shipmentNumber },
  { header: 'ROUTE', value: (r) => r.route },
  { header: 'DRIVER', value: (r) => r.driverName },
  { header: 'VEHICLE', value: (r) => r.vehicleRegistration },
  { header: 'STATUS', value: (r) => r.statusLabel },
  { header: 'LAST_EVENT', value: (r) => r.lastEventLabel },
  { header: 'LAST_EVENT_AT', value: (r) => r.lastEventAt },
  { header: 'STARTED', value: (r) => r.startedAtLabel },
];

export const BORDER_CLEARANCE_EXPORT_COLUMNS: LxExportColumn<BorderClearanceCaseRow>[] = [
  { header: 'CASE_NUMBER', value: (r) => r.caseNumber },
  { header: 'SHIPMENT_ID', value: (r) => r.shipmentId },
  { header: 'TRANSFER_ID', value: (r) => r.inventoryTransferId },
  { header: 'TRIP_ID', value: (r) => r.tripId ?? '' },
  { header: 'BORDER', value: (r) => r.borderName },
  { header: 'STATUS', value: (r) => r.statusLabel },
  { header: 'DOCUMENTS', value: (r) => r.documents.length },
  { header: 'CLEARED_AT', value: (r) => r.clearedAtLabel },
  { header: 'NOTES', value: (r) => r.notes },
];
