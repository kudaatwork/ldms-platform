package projectlx.co.zw.locationsmanagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import projectlx.co.zw.locationsmanagementservice.model.Address;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long>, JpaSpecificationExecutor<Address> {
    List<Address> findAllBySuburb_IdAndEntityStatusNot(Long suburbId, EntityStatus entityStatus);

    List<Address> findAllByCity_IdAndEntityStatusNot(Long cityId, EntityStatus entityStatus);

    List<Address> findAllByVillage_IdAndEntityStatusNot(Long villageId, EntityStatus entityStatus);

    Optional<Address> findByIdAndEntityStatusNot(Long id, EntityStatus entityStatus);

    @Query("SELECT a FROM Address a "
            + "LEFT JOIN FETCH a.city "
            + "LEFT JOIN FETCH a.suburb s "
            + "LEFT JOIN FETCH s.city "
            + "LEFT JOIN FETCH s.district d "
            + "LEFT JOIN FETCH d.province p "
            + "LEFT JOIN FETCH p.country "
            + "WHERE a.id = :id AND (a.entityStatus IS NULL OR a.entityStatus <> :excluded)")
    Optional<Address> findByIdFetchingContext(@Param("id") Long id, @Param("excluded") EntityStatus excluded);
}