UPDATE location_node ln
LEFT JOIN location_node parent_ln ON parent_ln.id = ln.parent_id
SET ln.district_id = parent_ln.district_id
WHERE ln.location_type = 'VILLAGE'
  AND ln.district_id IS NULL
  AND parent_ln.id IS NOT NULL
  AND parent_ln.district_id IS NOT NULL;

UPDATE suburb s
JOIN (
    SELECT district_id, MIN(id) AS city_node_id
    FROM location_node
    WHERE location_type = 'CITY'
      AND entity_status <> 'DELETED'
      AND district_id IS NOT NULL
    GROUP BY district_id
) city_map ON city_map.district_id = s.district_id
SET s.city_location_node_id = city_map.city_node_id
WHERE s.city_location_node_id IS NULL;

UPDATE address a
SET a.settlement_type = 'VILLAGE'
WHERE a.village_location_node_id IS NOT NULL
  AND (a.settlement_type IS NULL OR a.settlement_type <> 'VILLAGE');

UPDATE address a
SET a.settlement_type = 'SUBURB'
WHERE a.village_location_node_id IS NULL
  AND a.suburb_id IS NOT NULL
  AND (a.settlement_type IS NULL OR a.settlement_type <> 'SUBURB');

CREATE TABLE IF NOT EXISTS address_hierarchy_backfill_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    address_id BIGINT NOT NULL,
    issue VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

INSERT INTO address_hierarchy_backfill_audit (address_id, issue)
SELECT a.id, 'Address has neither suburb nor village settlement'
FROM address a
LEFT JOIN address_hierarchy_backfill_audit ahba
       ON ahba.address_id = a.id
      AND ahba.issue = 'Address has neither suburb nor village settlement'
WHERE a.suburb_id IS NULL
  AND a.village_location_node_id IS NULL
  AND ahba.id IS NULL;
