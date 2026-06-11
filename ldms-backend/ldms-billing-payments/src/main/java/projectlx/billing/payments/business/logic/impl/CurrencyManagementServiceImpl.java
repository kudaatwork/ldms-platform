package projectlx.billing.payments.business.logic.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.billing.payments.business.auditable.api.CountryCurrencySettingServiceAuditable;
import projectlx.billing.payments.business.auditable.api.ExchangeRateServiceAuditable;
import projectlx.billing.payments.business.auditable.api.OrganizationCurrencySettingServiceAuditable;
import projectlx.billing.payments.business.logic.api.CurrencyManagementService;
import projectlx.billing.payments.business.logic.support.BillingMapper;
import projectlx.billing.payments.business.logic.support.CurrencyConversionSupport;
import projectlx.billing.payments.business.validator.api.CurrencyManagementServiceValidator;
import projectlx.billing.payments.model.CountryCurrencySetting;
import projectlx.billing.payments.model.Currency;
import projectlx.billing.payments.model.ExchangeRate;
import projectlx.billing.payments.utils.dtos.ConversionResultDto;
import projectlx.billing.payments.utils.enums.ExchangeRateSource;
import projectlx.billing.payments.utils.enums.I18Code;
import projectlx.billing.payments.utils.requests.ConvertCurrencyRequest;
import projectlx.billing.payments.utils.requests.LockCurrencyConversionRequest;
import projectlx.billing.payments.utils.requests.CreateExchangeRateRequest;
import projectlx.billing.payments.business.logic.support.CallerOrganizationResolver;
import projectlx.billing.payments.business.logic.support.OrganizationCurrencySupport;
import projectlx.billing.payments.model.OrganizationCurrencySetting;
import projectlx.billing.payments.repository.OrganizationCurrencySettingRepository;
import projectlx.billing.payments.utils.dtos.OrganizationCurrencyContextDto;
import projectlx.billing.payments.utils.requests.SaveOrganizationCurrencySettingRequest;
import projectlx.billing.payments.utils.requests.SaveCountryCurrencySettingRequest;
import projectlx.billing.payments.utils.responses.CurrencyResponse;
import projectlx.billing.payments.repository.CountryCurrencySettingRepository;
import projectlx.billing.payments.repository.CurrencyRepository;
import projectlx.billing.payments.repository.ExchangeRateRepository;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Transactional
@RequiredArgsConstructor
public class CurrencyManagementServiceImpl implements CurrencyManagementService {

    private final CurrencyRepository currencyRepository;
    private final CountryCurrencySettingRepository countryCurrencySettingRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyConversionSupport currencyConversionSupport;
    private final OrganizationCurrencySupport organizationCurrencySupport;
    private final OrganizationCurrencySettingRepository organizationCurrencySettingRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final MessageService messageService;
    private final CountryCurrencySettingServiceAuditable countryCurrencySettingServiceAuditable;
    private final ExchangeRateServiceAuditable exchangeRateServiceAuditable;
    private final OrganizationCurrencySettingServiceAuditable organizationCurrencySettingServiceAuditable;
    private final CurrencyManagementServiceValidator currencyManagementServiceValidator;

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponse listCurrencies(Locale locale) {
        List<Currency> currencies = currencyRepository.findByEntityStatusNotOrderByCodeAsc(EntityStatus.DELETED);
        CurrencyResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_CURRENCY_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setCurrencyDtoList(currencies.stream().map(BillingMapper::toDto).toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponse listCountryCurrencySettings(Locale locale) {
        List<CountryCurrencySetting> settings =
                countryCurrencySettingRepository.findByEntityStatusNotOrderByCountryNameAsc(EntityStatus.DELETED);
        CurrencyResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_COUNTRY_CURRENCY_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setCountryCurrencySettingDtoList(settings.stream().map(BillingMapper::toDto).toList());
        return response;
    }

    @Override
    public CurrencyResponse saveCountryCurrencySetting(
            SaveCountryCurrencySettingRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = currencyManagementServiceValidator
                .isSaveCountryCurrencySettingRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_COUNTRY_CURRENCY_SAVE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        String baseCurrency = request.getBaseCurrencyCode().trim().toUpperCase();
        Optional<Currency> currency = currencyRepository.findByCodeAndEntityStatusNot(baseCurrency, EntityStatus.DELETED);
        if (currency.isEmpty()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_CURRENCY_NOT_FOUND.getCode(), new String[]{}, locale),
                    List.of("baseCurrencyCode"));
        }

        CountryCurrencySetting setting = countryCurrencySettingRepository
                .findByCountryIdAndEntityStatusNot(request.getCountryId(), EntityStatus.DELETED)
                .orElseGet(CountryCurrencySetting::new);

        boolean isNew = setting.getId() == null;
        setting.setCountryId(request.getCountryId());
        setting.setCountryName(request.getCountryName().trim());
        setting.setCountryIsoAlpha2(request.getCountryIsoAlpha2().trim().toUpperCase());
        setting.setBaseCurrencyCode(baseCurrency);
        setting.setEntityStatus(EntityStatus.ACTIVE);
        if (isNew) {
            setting.setCreatedAt(LocalDateTime.now());
            setting.setCreatedBy(username);
        } else {
            setting.setModifiedAt(LocalDateTime.now());
            setting.setModifiedBy(username);
        }

        CountryCurrencySetting saved = isNew
                ? countryCurrencySettingServiceAuditable.create(setting, locale, username)
                : countryCurrencySettingServiceAuditable.update(setting, locale, username);
        CurrencyResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_COUNTRY_CURRENCY_SAVE_SUCCESS.getCode(), new String[]{}, locale));
        response.setCountryCurrencySettingDto(BillingMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponse listExchangeRates(Locale locale) {
        List<ExchangeRate> rates = exchangeRateRepository.findByEntityStatusNotOrderByEffectiveFromDesc(EntityStatus.DELETED);
        CurrencyResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_EXCHANGE_RATE_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setExchangeRateDtoList(rates.stream().map(BillingMapper::toDto).toList());
        return response;
    }

    @Override
    public CurrencyResponse createExchangeRate(CreateExchangeRateRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = currencyManagementServiceValidator
                .isCreateExchangeRateRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_EXCHANGE_RATE_CREATE_INVALID.getCode(), new String[]{}, locale),
                    validation.getErrorMessages());
        }

        String from = request.getFromCurrencyCode().trim().toUpperCase();
        String to = request.getToCurrencyCode().trim().toUpperCase();
        LocalDateTime effectiveFrom = request.getEffectiveFrom() == null ? LocalDateTime.now() : request.getEffectiveFrom();

        exchangeRateRepository
                .findFirstByFromCurrencyCodeAndToCurrencyCodeAndEffectiveToIsNullAndEntityStatusNotOrderByEffectiveFromDesc(
                        from, to, EntityStatus.DELETED)
                .ifPresent(current -> {
                    current.setEffectiveTo(effectiveFrom);
                    current.setModifiedAt(LocalDateTime.now());
                    current.setModifiedBy(username);
                    exchangeRateServiceAuditable.update(current, locale, username);
                });

        ExchangeRate rate = new ExchangeRate();
        rate.setFromCurrencyCode(from);
        rate.setToCurrencyCode(to);
        rate.setRate(request.getRate());
        rate.setEffectiveFrom(effectiveFrom);
        rate.setSource(request.getSource() == null ? ExchangeRateSource.MANUAL : request.getSource());
        rate.setEntityStatus(EntityStatus.ACTIVE);
        rate.setCreatedAt(LocalDateTime.now());
        rate.setCreatedBy(username);

        ExchangeRate saved = exchangeRateServiceAuditable.create(rate, locale, username);
        CurrencyResponse response = success(201,
                messageService.getMessage(I18Code.MESSAGE_EXCHANGE_RATE_CREATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setExchangeRateDto(BillingMapper.toDto(saved));
        return response;
    }

    @Override
    public CurrencyResponse convert(ConvertCurrencyRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = currencyManagementServiceValidator
                .isConvertCurrencyRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale),
                    List.of("request"));
        }

        String toCurrency = StringUtils.hasText(request.getToCurrencyCode())
                ? request.getToCurrencyCode()
                : currencyConversionSupport.resolveBaseCurrencyForCountry(request.getCountryId());

        try {
            ConversionResultDto result = request.getTransactionDate() != null
                    ? currencyConversionSupport.convertAndLockOnDate(
                            request.getFromCurrencyCode(),
                            toCurrency,
                            request.getAmount(),
                            request.getTransactionDate(),
                            username)
                    : currencyConversionSupport.convertAndLock(
                            request.getFromCurrencyCode(),
                            toCurrency,
                            request.getAmount(),
                            LocalDateTime.now(),
                            username);
            CurrencyResponse response = success(200, "Conversion successful");
            response.setConversionResultDto(result);
            return response;
        } catch (IllegalStateException ex) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_EXCHANGE_RATE_NOT_FOUND.getCode(), new String[]{}, locale),
                    List.of(ex.getMessage()));
        }
    }

    @Override
    public CurrencyResponse lockConversionForOrganization(
            LockCurrencyConversionRequest request,
            Locale locale,
            String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = currencyManagementServiceValidator
                .isLockCurrencyConversionRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale),
                    List.of("organizationId", "transactionCurrencyCode", "amount"));
        }

        try {
            ConversionResultDto result = currencyConversionSupport.convertAndLockForOrganization(
                    request.getOrganizationId(),
                    request.getTransactionCurrencyCode(),
                    request.getAmount(),
                    request.getTransactionDate(),
                    username == null ? "SYSTEM" : username);
            CurrencyResponse response = success(200, "Conversion locked at transaction date");
            response.setConversionResultDto(result);
            return response;
        } catch (IllegalStateException ex) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_EXCHANGE_RATE_NOT_FOUND.getCode(), new String[]{}, locale),
                    List.of(ex.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponse getOrganizationCurrencySetting(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale),
                    List.of());
        }
        return buildOrganizationSettingResponse(organizationId, locale);
    }

    @Override
    public CurrencyResponse saveOrganizationCurrencySetting(
            SaveOrganizationCurrencySettingRequest request, Locale locale, String username) {

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale),
                    List.of());
        }

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = currencyManagementServiceValidator
                .isSaveOrganizationCurrencySettingRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_ORG_CURRENCY_SAVE_INVALID.getCode(), new String[]{}, locale),
                    List.of("functionalCurrencyCode"));
        }

        String functionalCurrency = request.getFunctionalCurrencyCode().trim().toUpperCase();
        Optional<Currency> currency = currencyRepository.findByCodeAndEntityStatusNot(functionalCurrency, EntityStatus.DELETED);
        if (currency.isEmpty()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_CURRENCY_NOT_FOUND.getCode(), new String[]{}, locale),
                    List.of("functionalCurrencyCode"));
        }

        OrganizationCurrencySetting setting = organizationCurrencySettingRepository
                .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                .orElseGet(OrganizationCurrencySetting::new);

        boolean isNew = setting.getId() == null;
        setting.setOrganizationId(organizationId);
        setting.setOrganizationName(StringUtils.hasText(request.getOrganizationName()) ? request.getOrganizationName().trim() : username);
        setting.setFunctionalCurrencyCode(functionalCurrency);
        setting.setCountryId(request.getCountryId());
        if (StringUtils.hasText(request.getCountryIsoAlpha2())) {
            setting.setCountryIsoAlpha2(request.getCountryIsoAlpha2().trim().toUpperCase());
        }
        setting.setEntityStatus(EntityStatus.ACTIVE);
        if (isNew) {
            setting.setCreatedAt(LocalDateTime.now());
            setting.setCreatedBy(username);
        } else {
            setting.setModifiedAt(LocalDateTime.now());
            setting.setModifiedBy(username);
        }

        OrganizationCurrencySetting saved = isNew
                ? organizationCurrencySettingServiceAuditable.create(setting, locale, username)
                : organizationCurrencySettingServiceAuditable.update(setting, locale, username);
        CurrencyResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_ORG_CURRENCY_SAVE_SUCCESS.getCode(), new String[]{}, locale));
        response.setOrganizationCurrencySettingDto(BillingMapper.toDto(saved, resolveCountryDefault(saved.getCountryId())));
        response.setOrganizationCurrencyContextDto(organizationCurrencySupport.buildContext(
                saved.getOrganizationId(), saved.getOrganizationName(), saved.getCountryId(), saved.getCountryIsoAlpha2()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponse getOrganizationCurrencyContext(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale),
                    List.of());
        }
        return buildOrganizationContextResponse(organizationId, username, locale);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponse getOrganizationCurrencyContextByOrganizationId(Long organizationId, Locale locale) {
        if (organizationId == null || organizationId < 1) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"organizationId"}, locale),
                    List.of());
        }
        return buildOrganizationContextResponse(organizationId, "SYSTEM", locale);
    }

    private CurrencyResponse buildOrganizationSettingResponse(Long organizationId, Locale locale) {
        Optional<OrganizationCurrencySetting> settingOpt = organizationCurrencySettingRepository
                .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED);
        CurrencyResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_ORG_CURRENCY_LIST_SUCCESS.getCode(), new String[]{}, locale));
        settingOpt.ifPresent(setting -> response.setOrganizationCurrencySettingDto(
                BillingMapper.toDto(setting, resolveCountryDefault(setting.getCountryId()))));
        return response;
    }

    private CurrencyResponse buildOrganizationContextResponse(Long organizationId, String organizationName, Locale locale) {
        Optional<OrganizationCurrencySetting> settingOpt = organizationCurrencySettingRepository
                .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED);
        Long countryId = settingOpt.map(OrganizationCurrencySetting::getCountryId).orElse(null);
        String countryIso = settingOpt.map(OrganizationCurrencySetting::getCountryIsoAlpha2).orElse(null);
        String orgName = settingOpt.map(OrganizationCurrencySetting::getOrganizationName).orElse(organizationName);

        OrganizationCurrencyContextDto context = organizationCurrencySupport.buildContext(
                organizationId, orgName, countryId, countryIso);

        CurrencyResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_ORG_CURRENCY_CONTEXT_SUCCESS.getCode(), new String[]{}, locale));
        response.setOrganizationCurrencyContextDto(context);
        response.setActiveExchangeRates(context.getActiveExchangeRates());
        return response;
    }

    private String resolveCountryDefault(Long countryId) {
        if (countryId == null) {
            return "USD";
        }
        return countryCurrencySettingRepository.findByCountryIdAndEntityStatusNot(countryId, EntityStatus.DELETED)
                .map(CountryCurrencySetting::getBaseCurrencyCode)
                .orElse("USD");
    }

    private CurrencyResponse success(int statusCode, String message) {
        CurrencyResponse response = new CurrencyResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(true);
        response.setMessage(message);
        return response;
    }

    private CurrencyResponse error(int statusCode, String message, List<String> errors) {
        CurrencyResponse response = new CurrencyResponse();
        response.setStatusCode(statusCode);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
