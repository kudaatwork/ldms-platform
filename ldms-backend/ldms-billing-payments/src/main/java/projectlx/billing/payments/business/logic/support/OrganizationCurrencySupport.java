package projectlx.billing.payments.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.billing.payments.model.Currency;
import projectlx.billing.payments.model.ExchangeRate;
import projectlx.billing.payments.model.OrganizationCurrencySetting;
import projectlx.billing.payments.repository.CountryCurrencySettingRepository;
import projectlx.billing.payments.repository.CurrencyRepository;
import projectlx.billing.payments.repository.ExchangeRateRepository;
import projectlx.billing.payments.repository.OrganizationCurrencySettingRepository;
import projectlx.billing.payments.utils.dtos.CurrencyDto;
import projectlx.billing.payments.utils.dtos.ExchangeRateDto;
import projectlx.billing.payments.utils.dtos.OrganizationCurrencyContextDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrganizationCurrencySupport {

    private static final String DEFAULT_FUNCTIONAL_CURRENCY = "USD";

    private final OrganizationCurrencySettingRepository organizationCurrencySettingRepository;
    private final CountryCurrencySettingRepository countryCurrencySettingRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public String resolveFunctionalCurrencyCode(Long organizationId, Long countryId) {
        if (organizationId != null) {
            Optional<OrganizationCurrencySetting> orgSetting = organizationCurrencySettingRepository
                    .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED);
            if (orgSetting.isPresent()) {
                return normalize(orgSetting.get().getFunctionalCurrencyCode());
            }
        }
        if (countryId != null) {
            return countryCurrencySettingRepository.findByCountryIdAndEntityStatusNot(countryId, EntityStatus.DELETED)
                    .map(s -> normalize(s.getBaseCurrencyCode()))
                    .orElse(DEFAULT_FUNCTIONAL_CURRENCY);
        }
        return DEFAULT_FUNCTIONAL_CURRENCY;
    }

    public OrganizationCurrencyContextDto buildContext(
            Long organizationId,
            String organizationName,
            Long countryId,
            String countryIsoAlpha2) {

        String countryDefault = countryId == null
                ? DEFAULT_FUNCTIONAL_CURRENCY
                : countryCurrencySettingRepository.findByCountryIdAndEntityStatusNot(countryId, EntityStatus.DELETED)
                        .map(s -> normalize(s.getBaseCurrencyCode()))
                        .orElse(DEFAULT_FUNCTIONAL_CURRENCY);

        Optional<OrganizationCurrencySetting> orgSetting = organizationId == null
                ? Optional.empty()
                : organizationCurrencySettingRepository.findByOrganizationIdAndEntityStatusNot(
                        organizationId, EntityStatus.DELETED);

        String functionalCode = orgSetting
                .map(s -> normalize(s.getFunctionalCurrencyCode()))
                .orElse(countryDefault);

        OrganizationCurrencyContextDto context = new OrganizationCurrencyContextDto();
        context.setOrganizationId(organizationId);
        context.setOrganizationName(organizationName);
        context.setFunctionalCurrencyCode(functionalCode);
        context.setCountryDefaultCurrencyCode(countryDefault);
        context.setCountryId(countryId);
        context.setCountryIsoAlpha2(countryIsoAlpha2);

        currencyRepository.findByCodeAndEntityStatusNot(functionalCode, EntityStatus.DELETED).ifPresent(c -> {
            context.setFunctionalCurrencyName(c.getName());
            context.setFunctionalCurrencySymbol(c.getSymbol());
            context.setFunctionalCurrencyDecimalPlaces(c.getDecimalPlaces());
        });

        List<Currency> currencies = currencyRepository.findByEntityStatusNotOrderByCodeAsc(EntityStatus.DELETED);
        context.setAvailableCurrencies(currencies.stream().map(BillingMapper::toDto).toList());
        context.setActiveExchangeRates(findActiveRatesFor(functionalCode));
        return context;
    }

    public List<ExchangeRateDto> findActiveRatesFor(String functionalCurrencyCode) {
        String functional = normalize(functionalCurrencyCode);
        LocalDateTime now = LocalDateTime.now();
        List<ExchangeRateDto> result = new ArrayList<>();

        for (ExchangeRate rate : exchangeRateRepository.findByEntityStatusNotOrderByEffectiveFromDesc(EntityStatus.DELETED)) {
            if (rate.getEffectiveTo() != null && !rate.getEffectiveTo().isAfter(now)) {
                continue;
            }
            if (!rate.getEffectiveFrom().isAfter(now)
                    && (functional.equals(normalize(rate.getFromCurrencyCode()))
                    || functional.equals(normalize(rate.getToCurrencyCode())))) {
                result.add(BillingMapper.toDto(rate));
            }
        }
        return result;
    }

    public Optional<CurrencyDto> findCurrency(String code) {
        return currencyRepository.findByCodeAndEntityStatusNot(normalize(code), EntityStatus.DELETED)
                .map(BillingMapper::toDto);
    }

    private String normalize(String code) {
        return StringUtils.hasText(code) ? code.trim().toUpperCase() : DEFAULT_FUNCTIONAL_CURRENCY;
    }
}
