import type { LogisticsRouteStopRow, WarehouseRow } from '../models/inventory.model';

/** Build ordered route stops for journey preview (origin → depots → destination). */
export function buildRoutePreviewStops(
  fromLocationId: number,
  toLocationId: number,
  enRouteDepotIds: number[],
  warehouses: WarehouseRow[],
  fallback?: { fromLabel?: string; toLabel?: string },
): LogisticsRouteStopRow[] {
  const warehouseById = new Map(warehouses.map((w) => [w.id, w]));
  const stops: LogisticsRouteStopRow[] = [];
  let sequence = 0;

  if (fromLocationId > 0) {
    stops.push({
      stopSequence: sequence++,
      stopType: 'ORIGIN',
      warehouseLocationId: fromLocationId,
      locationLabel:
        warehouseById.get(fromLocationId)?.name?.trim() || fallback?.fromLabel?.trim() || 'Origin',
    });
  }

  for (const depotId of enRouteDepotIds.filter((id) => id > 0)) {
    stops.push({
      stopSequence: sequence++,
      stopType: 'EN_ROUTE_DEPOT',
      warehouseLocationId: depotId,
      locationLabel: warehouseById.get(depotId)?.name?.trim() || `Warehouse #${depotId}`,
      warehouseName: warehouseById.get(depotId)?.name,
    });
  }

  if (toLocationId > 0) {
    stops.push({
      stopSequence: sequence,
      stopType: 'DESTINATION',
      warehouseLocationId: toLocationId,
      locationLabel: warehouseById.get(toLocationId)?.name?.trim() || fallback?.toLabel?.trim() || 'Destination',
    });
  }

  return stops;
}

export function transferEnRouteDepotCount(routeStops: LogisticsRouteStopRow[] | undefined): number {
  return (routeStops ?? []).filter((s) => s.stopType === 'EN_ROUTE_DEPOT').length;
}

export function transferRouteSummary(routeStops: LogisticsRouteStopRow[] | undefined): string {
  const depots = (routeStops ?? []).filter((s) => s.stopType === 'EN_ROUTE_DEPOT');
  if (!depots.length) {
    return 'Direct';
  }
  return depots
    .sort((a, b) => a.stopSequence - b.stopSequence)
    .map((s) => s.warehouseName?.trim() || s.locationLabel?.trim() || 'Depot')
    .join(' → ');
}
