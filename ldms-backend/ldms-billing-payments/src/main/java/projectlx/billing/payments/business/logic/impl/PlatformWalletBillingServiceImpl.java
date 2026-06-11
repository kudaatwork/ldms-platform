package projectlx.billing.payments.business.logic.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import projectlx.billing.payments.business.auditable.api.OrganizationBillingSettingServiceAuditable;
import projectlx.billing.payments.business.auditable.api.PlatformActionChargeServiceAuditable;
import projectlx.billing.payments.business.auditable.api.PlatformWalletServiceAuditable;
import projectlx.billing.payments.business.auditable.api.SubscriptionPackageServiceAuditable;
import projectlx.billing.payments.business.auditable.api.UsageChargeRecordServiceAuditable;
import projectlx.billing.payments.business.auditable.api.WalletDepositServiceAuditable;
import projectlx.billing.payments.business.auditable.api.WalletTransactionServiceAuditable;
import projectlx.billing.payments.business.logic.api.PlatformWalletBillingService;
import projectlx.billing.payments.business.logic.support.CallerOrganizationResolver;
import projectlx.billing.payments.business.logic.support.OrganizationCurrencySupport;
import projectlx.billing.payments.business.logic.support.PlatformWalletMapper;
import projectlx.billing.payments.business.validator.api.PlatformWalletBillingServiceValidator;
import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.billing.payments.model.PlatformActionCharge;
import projectlx.billing.payments.model.PlatformWallet;
import projectlx.billing.payments.model.SubscriptionPackage;
import projectlx.billing.payments.model.UsageChargeRecord;
import projectlx.billing.payments.model.WalletDeposit;
import projectlx.billing.payments.model.WalletTransaction;
import projectlx.billing.payments.repository.OrganizationBillingSettingRepository;
import projectlx.billing.payments.repository.PlatformActionChargeRepository;
import projectlx.billing.payments.repository.PlatformWalletRepository;
import projectlx.billing.payments.repository.SubscriptionPackageRepository;
import projectlx.billing.payments.repository.UsageChargeRecordRepository;
import projectlx.billing.payments.repository.WalletDepositRepository;
import projectlx.billing.payments.repository.WalletTransactionRepository;
import projectlx.billing.payments.utils.dtos.RecordPlatformUsageChargeResultDto;
import projectlx.billing.payments.utils.dtos.UsageChargeBreakdownDto;
import projectlx.billing.payments.utils.dtos.UsageChargeReportDto;
import projectlx.billing.payments.utils.enums.I18Code;
import projectlx.billing.payments.utils.enums.OrganizationBillingMode;
import projectlx.billing.payments.utils.enums.PlatformActionCategory;
import projectlx.billing.payments.utils.enums.WalletDepositStatus;
import projectlx.billing.payments.utils.enums.WalletTransactionType;
import projectlx.billing.payments.utils.requests.CreateWalletDepositRequest;
import projectlx.billing.payments.utils.requests.RecordPlatformUsageChargeRequest;
import projectlx.billing.payments.utils.requests.SaveOrganizationBillingSettingRequest;
import projectlx.billing.payments.utils.requests.SavePlatformActionChargeRequest;
import projectlx.billing.payments.utils.requests.SaveSubscriptionPackageRequest;
import projectlx.billing.payments.utils.requests.UsageChargeReportRequest;
import projectlx.billing.payments.utils.responses.PlatformWalletResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Transactional
@RequiredArgsConstructor
public class PlatformWalletBillingServiceImpl implements PlatformWalletBillingService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PlatformWalletRepository platformWalletRepository;
    private final OrganizationBillingSettingRepository organizationBillingSettingRepository;
    private final PlatformActionChargeRepository platformActionChargeRepository;
    private final SubscriptionPackageRepository subscriptionPackageRepository;
    private final WalletDepositRepository walletDepositRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UsageChargeRecordRepository usageChargeRecordRepository;
    private final CallerOrganizationResolver callerOrganizationResolver;
    private final OrganizationCurrencySupport organizationCurrencySupport;
    private final MessageService messageService;
    private final PlatformWalletServiceAuditable platformWalletServiceAuditable;
    private final OrganizationBillingSettingServiceAuditable organizationBillingSettingServiceAuditable;
    private final PlatformActionChargeServiceAuditable platformActionChargeServiceAuditable;
    private final SubscriptionPackageServiceAuditable subscriptionPackageServiceAuditable;
    private final WalletDepositServiceAuditable walletDepositServiceAuditable;
    private final WalletTransactionServiceAuditable walletTransactionServiceAuditable;
    private final UsageChargeRecordServiceAuditable usageChargeRecordServiceAuditable;
    private final PlatformWalletBillingServiceValidator platformWalletBillingServiceValidator;

    @Override
    @Transactional(readOnly = true)
    public PlatformWalletResponse getWalletSummary(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return orgError(locale);
        }
        return getWalletSummaryByOrganizationId(organizationId, locale);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformWalletResponse getWalletSummaryByOrganizationId(Long organizationId, Locale locale) {
        if (organizationId == null || organizationId < 1) {
            return error(400, messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"organizationId"}, locale),
                    List.of("organizationId"));
        }
        OrganizationBillingSetting setting = ensureBillingSetting(organizationId, "Organization", "SYSTEM");
        PlatformWallet wallet = ensureWallet(organizationId, setting.getOrganizationName(), resolveCurrency(organizationId), "SYSTEM");
        String packageName = resolvePackageName(setting.getSubscriptionPackageId());

        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_WALLET_SUMMARY_SUCCESS.getCode(), new String[]{}, locale));
        response.setPlatformWalletSummaryDto(PlatformWalletMapper.toSummaryDto(wallet, setting, packageName));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformWalletResponse getBillingSetting(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return orgError(locale);
        }
        OrganizationBillingSetting setting = ensureBillingSetting(organizationId, "Organization", username);
        String packageName = resolvePackageName(setting.getSubscriptionPackageId());
        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_BILLING_SETTING_SUCCESS.getCode(), new String[]{}, locale));
        response.setOrganizationBillingSettingDto(PlatformWalletMapper.toDto(setting, packageName));
        return response;
    }

    @Override
    public PlatformWalletResponse saveBillingSetting(
            SaveOrganizationBillingSettingRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = platformWalletBillingServiceValidator
                .isSaveBillingSettingRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400, messageService.getMessage(I18Code.MESSAGE_REQUEST_NULL.getCode(), new String[]{}, locale), List.of());
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return orgError(locale);
        }

        OrganizationBillingSetting setting = ensureBillingSetting(organizationId, "Organization", username);
        OrganizationBillingMode mode = parseBillingMode(request.getBillingMode());
        if (mode == null) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_BILLING_SETTING_INVALID.getCode(), new String[]{}, locale),
                    List.of("billingMode"));
        }

        setting.setBillingMode(mode);
        if (request.getLowBalanceThresholdCents() != null && request.getLowBalanceThresholdCents() >= 0) {
            setting.setLowBalanceThresholdCents(request.getLowBalanceThresholdCents());
        }

        if (mode == OrganizationBillingMode.PREMIUM_SUBSCRIPTION) {
            if (request.getSubscriptionPackageId() == null || request.getSubscriptionPackageId() < 1) {
                return error(400,
                        messageService.getMessage(I18Code.MESSAGE_BILLING_SETTING_INVALID.getCode(), new String[]{}, locale),
                        List.of("subscriptionPackageId"));
            }
            SubscriptionPackage pkg = subscriptionPackageRepository
                    .findByIdAndEntityStatusNot(request.getSubscriptionPackageId(), EntityStatus.DELETED)
                    .orElse(null);
            if (pkg == null || !Boolean.TRUE.equals(pkg.getActive())) {
                return error(404,
                        messageService.getMessage(I18Code.MESSAGE_SUBSCRIPTION_PACKAGE_NOT_FOUND.getCode(), new String[]{}, locale),
                        List.of("subscriptionPackageId"));
            }
            setting.setSubscriptionPackageId(pkg.getId());
            if (setting.getSubscriptionStartedAt() == null) {
                setting.setSubscriptionStartedAt(LocalDateTime.now());
            }
            setting.setSubscriptionRenewsAt(LocalDateTime.now().plusMonths(1));
        } else {
            setting.setSubscriptionPackageId(null);
            setting.setSubscriptionStartedAt(null);
            setting.setSubscriptionRenewsAt(null);
        }

        setting.setModifiedAt(LocalDateTime.now());
        setting.setModifiedBy(username);
        OrganizationBillingSetting saved = organizationBillingSettingServiceAuditable.update(setting, locale, username);
        String packageName = resolvePackageName(saved.getSubscriptionPackageId());

        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_BILLING_SETTING_SAVE_SUCCESS.getCode(), new String[]{}, locale));
        response.setOrganizationBillingSettingDto(PlatformWalletMapper.toDto(saved, packageName));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformWalletResponse listSubscriptionPackages(Locale locale, boolean activeOnly) {
        List<SubscriptionPackage> packages = activeOnly
                ? subscriptionPackageRepository.findByEntityStatusNotAndActiveTrueOrderBySortOrderAsc(EntityStatus.DELETED)
                : subscriptionPackageRepository.findByEntityStatusNotOrderBySortOrderAsc(EntityStatus.DELETED);
        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_SUBSCRIPTION_PACKAGE_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setSubscriptionPackageDtoList(packages.stream().map(PlatformWalletMapper::toDto).toList());
        return response;
    }

    @Override
    public PlatformWalletResponse createWalletDeposit(CreateWalletDepositRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = platformWalletBillingServiceValidator
                .isCreateWalletDepositRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_WALLET_DEPOSIT_INVALID.getCode(), new String[]{}, locale),
                    List.of("amountCents"));
        }

        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return orgError(locale);
        }

        OrganizationBillingSetting setting = ensureBillingSetting(organizationId, "Organization", username);
        if (setting.getBillingMode() != OrganizationBillingMode.PREPAID_WALLET) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_WALLET_DEPOSIT_PREMIUM_ONLY.getCode(), new String[]{}, locale),
                    List.of("billingMode"));
        }

        WalletDeposit deposit = new WalletDeposit();
        deposit.setOrganizationId(organizationId);
        deposit.setAmountCents(request.getAmountCents());
        deposit.setCurrencyCode(StringUtils.hasText(request.getCurrencyCode())
                ? request.getCurrencyCode().trim().toUpperCase()
                : resolveCurrency(organizationId));
        deposit.setReferenceNumber(trimToNull(request.getReferenceNumber()));
        deposit.setNotes(trimToNull(request.getNotes()));
        deposit.setProofDocumentId(request.getProofDocumentId());
        deposit.setStatus(WalletDepositStatus.PENDING);
        deposit.setEntityStatus(EntityStatus.ACTIVE);
        deposit.setCreatedAt(LocalDateTime.now());
        deposit.setCreatedBy(username);

        WalletDeposit saved = walletDepositServiceAuditable.create(deposit, locale, username);
        PlatformWalletResponse response = success(201,
                messageService.getMessage(I18Code.MESSAGE_WALLET_DEPOSIT_CREATE_SUCCESS.getCode(), new String[]{}, locale));
        response.setWalletDepositDto(PlatformWalletMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformWalletResponse listWalletDeposits(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return orgError(locale);
        }
        List<WalletDeposit> deposits = walletDepositRepository
                .findByOrganizationIdAndEntityStatusNotOrderByCreatedAtDesc(organizationId, EntityStatus.DELETED);
        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_WALLET_DEPOSIT_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setWalletDepositDtoList(deposits.stream().map(PlatformWalletMapper::toDto).toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformWalletResponse listRecentWalletTransactions(Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return orgError(locale);
        }
        List<WalletTransaction> transactions = walletTransactionRepository
                .findTop50ByOrganizationIdAndEntityStatusNotOrderByCreatedAtDesc(organizationId, EntityStatus.DELETED);
        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_WALLET_TX_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setWalletTransactionDtoList(transactions.stream().map(PlatformWalletMapper::toDto).toList());
        return response;
    }

    @Override
    public PlatformWalletResponse confirmWalletDeposit(Long depositId, Locale locale, String username) {
        if (depositId == null || depositId < 1) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_FIELD_REQUIRED.getCode(), new String[]{"depositId"}, locale),
                    List.of("depositId"));
        }
        WalletDeposit deposit = walletDepositRepository.findById(depositId).orElse(null);
        if (deposit == null || deposit.getEntityStatus() == EntityStatus.DELETED) {
            return error(404,
                    messageService.getMessage(I18Code.MESSAGE_WALLET_DEPOSIT_NOT_FOUND.getCode(), new String[]{}, locale),
                    List.of("depositId"));
        }
        if (deposit.getStatus() != WalletDepositStatus.PENDING) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_WALLET_DEPOSIT_INVALID.getCode(), new String[]{}, locale),
                    List.of("status"));
        }

        OrganizationBillingSetting setting = ensureBillingSetting(deposit.getOrganizationId(), "Organization", username);
        PlatformWallet wallet = ensureWallet(
                deposit.getOrganizationId(),
                setting.getOrganizationName(),
                deposit.getCurrencyCode(),
                username);

        PlatformWallet locked = platformWalletRepository
                .findByOrganizationIdForUpdate(deposit.getOrganizationId(), EntityStatus.DELETED)
                .orElse(wallet);

        long newBalance = locked.getBalanceCents() + deposit.getAmountCents();
        locked.setBalanceCents(newBalance);
        locked.setModifiedAt(LocalDateTime.now());
        locked.setModifiedBy(username);
        platformWalletServiceAuditable.update(locked, locale, username);

        WalletTransaction tx = new WalletTransaction();
        tx.setOrganizationId(deposit.getOrganizationId());
        tx.setTransactionType(WalletTransactionType.DEPOSIT);
        tx.setAmountCents(deposit.getAmountCents());
        tx.setBalanceAfterCents(newBalance);
        tx.setReferenceType("WALLET_DEPOSIT");
        tx.setReferenceId(deposit.getId());
        tx.setDescription("Wallet deposit confirmed");
        tx.setEntityStatus(EntityStatus.ACTIVE);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setCreatedBy(username);
        walletTransactionServiceAuditable.create(tx, locale, username);

        deposit.setStatus(WalletDepositStatus.CONFIRMED);
        deposit.setModifiedAt(LocalDateTime.now());
        deposit.setModifiedBy(username);
        WalletDeposit saved = walletDepositServiceAuditable.update(deposit, locale, username);

        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_WALLET_DEPOSIT_CONFIRM_SUCCESS.getCode(), new String[]{}, locale));
        response.setWalletDepositDto(PlatformWalletMapper.toDto(saved));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformWalletResponse listPendingDeposits(Locale locale) {
        List<WalletDeposit> deposits = walletDepositRepository
                .findByStatusAndEntityStatusNotOrderByCreatedAtDesc(WalletDepositStatus.PENDING, EntityStatus.DELETED);
        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_WALLET_DEPOSIT_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setWalletDepositDtoList(deposits.stream().map(PlatformWalletMapper::toDto).toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformWalletResponse listActionCharges(Locale locale) {
        List<PlatformActionCharge> charges = platformActionChargeRepository
                .findByEntityStatusNotOrderByCategoryAscDisplayNameAsc(EntityStatus.DELETED);
        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_ACTION_CHARGE_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setPlatformActionChargeDtoList(charges.stream().map(PlatformWalletMapper::toDto).toList());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformWalletResponse listActiveActionCharges(Locale locale) {
        List<PlatformActionCharge> charges = platformActionChargeRepository
                .findByEntityStatusNotOrderByCategoryAscDisplayNameAsc(EntityStatus.DELETED)
                .stream()
                .filter(charge -> Boolean.TRUE.equals(charge.getActive()))
                .toList();
        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_ACTION_CHARGE_LIST_SUCCESS.getCode(), new String[]{}, locale));
        response.setPlatformActionChargeDtoList(charges.stream().map(PlatformWalletMapper::toDto).toList());
        return response;
    }

    @Override
    public PlatformWalletResponse saveActionCharge(
            SavePlatformActionChargeRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = platformWalletBillingServiceValidator
                .isSaveActionChargeRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_ACTION_CHARGE_INVALID.getCode(), new String[]{}, locale),
                    List.of("actionCode", "chargeCents"));
        }

        String actionCode = request.getActionCode().trim().toUpperCase();
        PlatformActionCharge charge = platformActionChargeRepository
                .findByActionCodeAndEntityStatusNot(actionCode, EntityStatus.DELETED)
                .orElseGet(PlatformActionCharge::new);

        boolean isNew = charge.getId() == null;
        charge.setActionCode(actionCode);
        charge.setDisplayName(StringUtils.hasText(request.getDisplayName()) ? request.getDisplayName().trim() : actionCode);
        charge.setDescription(trimToNull(request.getDescription()));
        charge.setChargeCents(Math.max(0L, request.getChargeCents()));
        if (StringUtils.hasText(request.getCategory())) {
            try {
                charge.setCategory(PlatformActionCategory.valueOf(request.getCategory().trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                charge.setCategory(PlatformActionCategory.GENERAL);
            }
        }
        if (request.getActive() != null) {
            charge.setActive(request.getActive());
        }
        charge.setEntityStatus(EntityStatus.ACTIVE);
        if (isNew) {
            charge.setCreatedAt(LocalDateTime.now());
            charge.setCreatedBy(username);
        } else {
            charge.setModifiedAt(LocalDateTime.now());
            charge.setModifiedBy(username);
        }

        PlatformActionCharge saved = isNew
                ? platformActionChargeServiceAuditable.create(charge, locale, username)
                : platformActionChargeServiceAuditable.update(charge, locale, username);
        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_ACTION_CHARGE_SAVE_SUCCESS.getCode(), new String[]{}, locale));
        response.setPlatformActionChargeDto(PlatformWalletMapper.toDto(saved));
        return response;
    }

    @Override
    public PlatformWalletResponse saveSubscriptionPackage(
            SaveSubscriptionPackageRequest request, Locale locale, String username) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = platformWalletBillingServiceValidator
                .isSaveSubscriptionPackageRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_SUBSCRIPTION_PACKAGE_INVALID.getCode(), new String[]{}, locale),
                    List.of("code", "name", "monthlyPriceCents"));
        }

        String code = request.getCode().trim().toUpperCase();
        SubscriptionPackage pkg = Optional.ofNullable(request.getId())
                .flatMap(id -> subscriptionPackageRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED))
                .or(() -> subscriptionPackageRepository.findByCodeAndEntityStatusNot(code, EntityStatus.DELETED))
                .orElseGet(SubscriptionPackage::new);

        boolean isNew = pkg.getId() == null;
        pkg.setCode(code);
        pkg.setName(request.getName().trim());
        pkg.setDescription(trimToNull(request.getDescription()));
        pkg.setMonthlyPriceCents(Math.max(0L, request.getMonthlyPriceCents()));
        pkg.setCurrencyCode(StringUtils.hasText(request.getCurrencyCode())
                ? request.getCurrencyCode().trim().toUpperCase()
                : "USD");
        if (request.getSortOrder() != null) {
            pkg.setSortOrder(request.getSortOrder());
        }
        if (request.getFeatured() != null) {
            pkg.setFeatured(request.getFeatured());
        }
        if (request.getActive() != null) {
            pkg.setActive(request.getActive());
        }
        pkg.setEntityStatus(EntityStatus.ACTIVE);
        if (isNew) {
            pkg.setCreatedAt(LocalDateTime.now());
            pkg.setCreatedBy(username);
        } else {
            pkg.setModifiedAt(LocalDateTime.now());
            pkg.setModifiedBy(username);
        }

        SubscriptionPackage saved = isNew
                ? subscriptionPackageServiceAuditable.create(pkg, locale, username)
                : subscriptionPackageServiceAuditable.update(pkg, locale, username);
        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_SUBSCRIPTION_PACKAGE_SAVE_SUCCESS.getCode(), new String[]{}, locale));
        response.setSubscriptionPackageDto(PlatformWalletMapper.toDto(saved));
        return response;
    }

    @Override
    public PlatformWalletResponse recordUsageCharge(
            RecordPlatformUsageChargeRequest request, Locale locale, String actor) {

        // ============================================================
        // STEP 1: Validate request
        // ============================================================
        ValidatorDto validation = platformWalletBillingServiceValidator
                .isRecordUsageChargeRequestValid(request, locale);
        if (!validation.getSuccess()) {
            return error(400,
                    messageService.getMessage(I18Code.MESSAGE_USAGE_CHARGE_INVALID.getCode(), new String[]{}, locale),
                    List.of("organizationId", "actionCode"));
        }

        String actionCode = request.getActionCode().trim().toUpperCase();
        PlatformActionCharge catalog = platformActionChargeRepository
                .findByActionCodeAndEntityStatusNot(actionCode, EntityStatus.DELETED)
                .orElse(null);
        if (catalog == null || !Boolean.TRUE.equals(catalog.getActive())) {
            return error(404,
                    messageService.getMessage(I18Code.MESSAGE_ACTION_CHARGE_NOT_FOUND.getCode(), new String[]{}, locale),
                    List.of("actionCode"));
        }

        Long chargeCents = catalog.getChargeCents() != null ? catalog.getChargeCents() : 0L;
        OrganizationBillingSetting setting = ensureBillingSetting(request.getOrganizationId(), "Organization", actor);
        boolean prepaid = setting.getBillingMode() == OrganizationBillingMode.PREPAID_WALLET;

        UsageChargeRecord record = new UsageChargeRecord();
        record.setOrganizationId(request.getOrganizationId());
        record.setBillingMode(setting.getBillingMode());
        record.setActionCode(actionCode);
        record.setActionDisplayName(catalog.getDisplayName());
        record.setChargeCents(chargeCents);
        record.setTripId(request.getTripId());
        record.setSeasonId(request.getSeasonId());
        record.setReferenceType(trimToNull(request.getReferenceType()));
        record.setReferenceId(request.getReferenceId());
        record.setServiceName(trimToNull(request.getServiceName()));
        record.setTraceId(trimToNull(request.getTraceId()));
        record.setEntityStatus(EntityStatus.ACTIVE);
        record.setCreatedAt(LocalDateTime.now());
        record.setCreatedBy(actor);

        RecordPlatformUsageChargeResultDto result = new RecordPlatformUsageChargeResultDto();
        result.setChargeCents(chargeCents);
        result.setBillingMode(setting.getBillingMode().name());

        if (prepaid && chargeCents > 0) {
            PlatformWallet wallet = ensureWallet(
                    request.getOrganizationId(),
                    setting.getOrganizationName(),
                    resolveCurrency(request.getOrganizationId()),
                    actor);
            PlatformWallet locked = platformWalletRepository
                    .findByOrganizationIdForUpdate(request.getOrganizationId(), EntityStatus.DELETED)
                    .orElse(wallet);

            if (locked.getBalanceCents() < chargeCents) {
                result.setAllowed(false);
                result.setDeducted(false);
                result.setBalanceAfterCents(locked.getBalanceCents());
                result.setMessage(messageService.getMessage(
                        I18Code.MESSAGE_WALLET_INSUFFICIENT_BALANCE.getCode(), new String[]{}, locale));

                PlatformWalletResponse response = error(402,
                        messageService.getMessage(I18Code.MESSAGE_WALLET_INSUFFICIENT_BALANCE.getCode(), new String[]{}, locale),
                        List.of("balanceCents"));
                response.setRecordPlatformUsageChargeResultDto(result);
                return response;
            }

            long newBalance = locked.getBalanceCents() - chargeCents;
            locked.setBalanceCents(newBalance);
            locked.setModifiedAt(LocalDateTime.now());
            locked.setModifiedBy(actor);
            platformWalletServiceAuditable.update(locked, locale, actor);

            WalletTransaction tx = new WalletTransaction();
            tx.setOrganizationId(request.getOrganizationId());
            tx.setTransactionType(WalletTransactionType.CHARGE);
            tx.setAmountCents(chargeCents);
            tx.setBalanceAfterCents(newBalance);
            tx.setActionCode(actionCode);
            tx.setReferenceType(trimToNull(request.getReferenceType()));
            tx.setReferenceId(request.getReferenceId());
            tx.setTripId(request.getTripId());
            tx.setSeasonId(request.getSeasonId());
            tx.setDescription(catalog.getDisplayName());
            tx.setEntityStatus(EntityStatus.ACTIVE);
            tx.setCreatedAt(LocalDateTime.now());
            tx.setCreatedBy(actor);
            walletTransactionServiceAuditable.create(tx, locale, actor);

            record.setDeducted(true);
            result.setAllowed(true);
            result.setDeducted(true);
            result.setBalanceAfterCents(newBalance);
        } else {
            record.setDeducted(false);
            result.setAllowed(true);
            result.setDeducted(false);
            PlatformWallet wallet = platformWalletRepository
                    .findByOrganizationIdAndEntityStatusNot(request.getOrganizationId(), EntityStatus.DELETED)
                    .orElse(null);
            result.setBalanceAfterCents(wallet != null ? wallet.getBalanceCents() : 0L);
        }

        usageChargeRecordServiceAuditable.create(record, locale, actor);
        result.setMessage(messageService.getMessage(I18Code.MESSAGE_USAGE_CHARGE_RECORDED.getCode(), new String[]{}, locale));

        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_USAGE_CHARGE_RECORDED.getCode(), new String[]{}, locale));
        response.setRecordPlatformUsageChargeResultDto(result);
        response.setUsageChargeRecordDto(PlatformWalletMapper.toDto(record));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformWalletResponse getUsageReport(UsageChargeReportRequest request, Locale locale, String username) {
        Long organizationId = callerOrganizationResolver.requireCallerOrganizationId(username);
        if (organizationId == null) {
            return orgError(locale);
        }

        LocalDateTime from = parseDateTime(request != null ? request.getFrom() : null, LocalDate.now().minusDays(30).atStartOfDay());
        LocalDateTime to = parseDateTime(request != null ? request.getTo() : null, LocalDateTime.now().plusDays(1));
        Long tripId = request != null ? request.getTripId() : null;
        Long seasonId = request != null ? request.getSeasonId() : null;

        List<UsageChargeRecord> records = usageChargeRecordRepository.findForReport(
                organizationId, from, to, tripId, seasonId, EntityStatus.DELETED);

        OrganizationBillingSetting setting = ensureBillingSetting(organizationId, "Organization", username);
        Map<String, UsageChargeBreakdownDto> breakdownMap = new LinkedHashMap<>();
        long total = 0L;
        long deducted = 0L;

        for (UsageChargeRecord record : records) {
            total += record.getChargeCents();
            if (Boolean.TRUE.equals(record.getDeducted())) {
                deducted += record.getChargeCents();
            }
            UsageChargeBreakdownDto row = breakdownMap.computeIfAbsent(record.getActionCode(), code -> {
                UsageChargeBreakdownDto dto = new UsageChargeBreakdownDto();
                dto.setActionCode(code);
                dto.setActionDisplayName(record.getActionDisplayName());
                dto.setTotalChargeCents(0L);
                dto.setEventCount(0L);
                return dto;
            });
            row.setTotalChargeCents(row.getTotalChargeCents() + record.getChargeCents());
            row.setEventCount(row.getEventCount() + 1);
        }

        Map<LocalDate, Long> daily = new HashMap<>();
        for (UsageChargeRecord record : records) {
            LocalDate day = record.getCreatedAt().toLocalDate();
            daily.merge(day, record.getChargeCents(), Long::sum);
        }

        List<String> dailyLabels = new ArrayList<>();
        List<Long> dailyTotals = new ArrayList<>();
        LocalDate cursor = from.toLocalDate();
        LocalDate endDay = to.toLocalDate().minusDays(1);
        while (!cursor.isAfter(endDay)) {
            dailyLabels.add(cursor.toString());
            dailyTotals.add(daily.getOrDefault(cursor, 0L));
            cursor = cursor.plusDays(1);
        }

        UsageChargeReportDto report = new UsageChargeReportDto();
        report.setOrganizationId(organizationId);
        report.setBillingMode(setting.getBillingMode().name());
        report.setTripId(tripId);
        report.setSeasonId(seasonId);
        report.setPeriodFrom(from.format(ISO));
        report.setPeriodTo(to.format(ISO));
        report.setTotalChargeCents(total);
        report.setDeductedChargeCents(deducted);
        report.setHypotheticalChargeCents(total);
        report.setBreakdown(new ArrayList<>(breakdownMap.values()));
        report.setRecords(records.stream().map(PlatformWalletMapper::toDto).toList());
        report.setDailyLabels(dailyLabels);
        report.setDailyTotalsCents(dailyTotals);

        PlatformWalletResponse response = success(200,
                messageService.getMessage(I18Code.MESSAGE_USAGE_REPORT_SUCCESS.getCode(), new String[]{}, locale));
        response.setUsageChargeReportDto(report);
        return response;
    }

    private OrganizationBillingSetting ensureBillingSetting(Long organizationId, String orgName, String actor) {
        return organizationBillingSettingRepository
                .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                .orElseGet(() -> {
                    OrganizationBillingSetting setting = new OrganizationBillingSetting();
                    setting.setOrganizationId(organizationId);
                    setting.setOrganizationName(orgName);
                    setting.setBillingMode(OrganizationBillingMode.PREPAID_WALLET);
                    setting.setLowBalanceThresholdCents(500L);
                    setting.setEntityStatus(EntityStatus.ACTIVE);
                    setting.setCreatedAt(LocalDateTime.now());
                    setting.setCreatedBy(actor);
                    return organizationBillingSettingServiceAuditable.create(setting, null, actor);
                });
    }

    private PlatformWallet ensureWallet(Long organizationId, String orgName, String currencyCode, String actor) {
        return platformWalletRepository
                .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                .orElseGet(() -> {
                    PlatformWallet wallet = new PlatformWallet();
                    wallet.setOrganizationId(organizationId);
                    wallet.setOrganizationName(orgName);
                    wallet.setBalanceCents(0L);
                    wallet.setCurrencyCode(currencyCode);
                    wallet.setEntityStatus(EntityStatus.ACTIVE);
                    wallet.setCreatedAt(LocalDateTime.now());
                    wallet.setCreatedBy(actor);
                    return platformWalletServiceAuditable.create(wallet, null, actor);
                });
    }

    private String resolveCurrency(Long organizationId) {
        return organizationCurrencySupport.resolveFunctionalCurrencyCode(organizationId, null);
    }

    private String resolvePackageName(Long packageId) {
        if (packageId == null) {
            return null;
        }
        return subscriptionPackageRepository.findByIdAndEntityStatusNot(packageId, EntityStatus.DELETED)
                .map(SubscriptionPackage::getName)
                .orElse(null);
    }

    private OrganizationBillingMode parseBillingMode(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return OrganizationBillingMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String raw, LocalDateTime fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        try {
            if (raw.length() <= 10) {
                return LocalDate.parse(raw).atStartOfDay();
            }
            return LocalDateTime.parse(raw, ISO);
        } catch (DateTimeParseException ex) {
            return fallback;
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private PlatformWalletResponse orgError(Locale locale) {
        return error(400,
                messageService.getMessage(I18Code.MESSAGE_ORGANIZATION_UNRESOLVED.getCode(), new String[]{}, locale),
                List.of("organizationId"));
    }

    private PlatformWalletResponse success(int statusCode, String message) {
        PlatformWalletResponse response = new PlatformWalletResponse();
        response.setSuccess(true);
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }

    private PlatformWalletResponse error(int statusCode, String message, List<String> errors) {
        PlatformWalletResponse response = new PlatformWalletResponse();
        response.setSuccess(false);
        response.setStatusCode(statusCode);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }
}
