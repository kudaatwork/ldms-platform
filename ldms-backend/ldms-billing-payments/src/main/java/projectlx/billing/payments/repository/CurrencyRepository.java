package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.Currency;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    List<Currency> findByEntityStatusNotOrderByCodeAsc(EntityStatus entityStatus);

    Optional<Currency> findByCodeAndEntityStatusNot(String code, EntityStatus entityStatus);
}
