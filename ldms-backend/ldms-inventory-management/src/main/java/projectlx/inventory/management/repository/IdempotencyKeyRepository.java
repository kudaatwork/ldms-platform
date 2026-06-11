package projectlx.inventory.management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.inventory.management.model.IdempotencyKey;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByKeyValueAndEntityStatusNot(String keyValue, EntityStatus entityStatus);
    Optional<IdempotencyKey> findByKeyValue(String keyValue);
    boolean existsByKeyValue(String keyValue);
}
