package projectlx.billing.payments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.billing.payments.model.CountryCurrencySetting;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface CountryCurrencySettingRepository extends JpaRepository<CountryCurrencySetting, Long> {

    List<CountryCurrencySetting> findByEntityStatusNotOrderByCountryNameAsc(EntityStatus entityStatus);

    Optional<CountryCurrencySetting> findByCountryIdAndEntityStatusNot(Long countryId, EntityStatus entityStatus);
}
