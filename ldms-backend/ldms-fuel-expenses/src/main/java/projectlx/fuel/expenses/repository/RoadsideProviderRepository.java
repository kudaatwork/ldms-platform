package projectlx.fuel.expenses.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.fuel.expenses.model.RoadsideProvider;

import java.math.BigDecimal;
import java.util.List;

public interface RoadsideProviderRepository extends JpaRepository<RoadsideProvider, Long> {

    List<RoadsideProvider> findByEntityStatusNotOrderByNameAsc(EntityStatus entityStatus);

    @Query("""
            SELECT rp FROM RoadsideProvider rp
            WHERE rp.entityStatus <> projectlx.co.zw.shared_library.utils.enums.EntityStatus.DELETED
              AND rp.verified = true
              AND rp.latitude BETWEEN :minLat AND :maxLat
              AND rp.longitude BETWEEN :minLng AND :maxLng
            ORDER BY rp.name ASC
            """)
    List<RoadsideProvider> findInBoundingBox(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng);
}
