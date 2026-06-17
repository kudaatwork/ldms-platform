-- Keep only the newest active trip per shipment (cleans up double-start races).
UPDATE trip t
    INNER JOIN (SELECT shipment_id, MAX(id) AS keep_trip_id
                FROM trip
                WHERE entity_status <> 'DELETED'
                  AND status NOT IN ('DELIVERED', 'CANCELLED')
                GROUP BY shipment_id) latest ON latest.shipment_id = t.shipment_id
SET t.status        = 'CANCELLED',
    t.modified_at   = UTC_TIMESTAMP(6),
    t.modified_by   = 'SYSTEM'
WHERE t.entity_status <> 'DELETED'
  AND t.status NOT IN ('DELIVERED', 'CANCELLED')
  AND t.id <> latest.keep_trip_id;

UPDATE trip_route_plan p
    INNER JOIN trip t ON t.id = p.trip_id
SET p.simulation_active = 0,
    p.modified_at       = UTC_TIMESTAMP(6),
    p.modified_by       = 'SYSTEM'
WHERE t.status = 'CANCELLED'
  AND p.simulation_active = 1
  AND p.entity_status <> 'DELETED';
