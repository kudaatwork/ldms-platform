package projectlx.billing.payments.business.logic.support;

import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.billing.payments.model.PlatformActionCharge;
import projectlx.billing.payments.model.PlatformWallet;
import projectlx.billing.payments.model.SubscriptionPackage;
import projectlx.billing.payments.model.UsageChargeRecord;
import projectlx.billing.payments.model.WalletDeposit;
import projectlx.billing.payments.model.WalletTransaction;
import projectlx.billing.payments.utils.enums.OrganizationBillingMode;
import projectlx.billing.payments.utils.dtos.OrganizationBillingSettingDto;
import projectlx.billing.payments.utils.dtos.PlatformActionChargeDto;
import projectlx.billing.payments.utils.dtos.PlatformWalletSummaryDto;
import projectlx.billing.payments.utils.dtos.SubscriptionPackageDto;
import projectlx.billing.payments.utils.dtos.UsageChargeRecordDto;
import projectlx.billing.payments.utils.dtos.WalletDepositDto;
import projectlx.billing.payments.utils.dtos.WalletTransactionDto;

import java.time.format.DateTimeFormatter;

public final class PlatformWalletMapper {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private PlatformWalletMapper() {
    }

    public static PlatformActionChargeDto toDto(PlatformActionCharge entity) {
        PlatformActionChargeDto dto = new PlatformActionChargeDto();
        dto.setId(entity.getId());
        dto.setActionCode(entity.getActionCode());
        dto.setDisplayName(entity.getDisplayName());
        dto.setDescription(entity.getDescription());
        dto.setChargeCents(entity.getChargeCents());
        dto.setCategory(entity.getCategory() != null ? entity.getCategory().name() : null);
        dto.setBillingTier(entity.getBillingTier() != null ? entity.getBillingTier().name() : null);
        dto.setActive(entity.getActive());
        return dto;
    }

    public static SubscriptionPackageDto toDto(SubscriptionPackage entity) {
        SubscriptionPackageDto dto = new SubscriptionPackageDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setMonthlyPriceCents(entity.getMonthlyPriceCents());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setIncludedHeavyCredits(entity.getIncludedHeavyCredits());
        dto.setIncludedStandardCredits(entity.getIncludedStandardCredits());
        dto.setIncludedLightCredits(entity.getIncludedLightCredits());
        dto.setIncludedTrackingDayCredits(entity.getIncludedTrackingDayCredits());
        dto.setSortOrder(entity.getSortOrder());
        dto.setFeatured(entity.getFeatured());
        dto.setActive(entity.getActive());
        return dto;
    }

    public static OrganizationBillingSettingDto toDto(OrganizationBillingSetting entity, String packageName) {
        OrganizationBillingSettingDto dto = new OrganizationBillingSettingDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setOrganizationName(entity.getOrganizationName());
        dto.setBillingMode(entity.getBillingMode() != null ? entity.getBillingMode().name() : null);
        dto.setSubscriptionPackageId(entity.getSubscriptionPackageId());
        dto.setSubscriptionPackageName(packageName);
        if (entity.getSubscriptionStartedAt() != null) {
            dto.setSubscriptionStartedAt(entity.getSubscriptionStartedAt().format(ISO));
        }
        if (entity.getSubscriptionRenewsAt() != null) {
            dto.setSubscriptionRenewsAt(entity.getSubscriptionRenewsAt().format(ISO));
        }
        dto.setLowBalanceThresholdCents(entity.getLowBalanceThresholdCents());
        return dto;
    }

    public static PlatformWalletSummaryDto toSummaryDto(
            PlatformWallet wallet,
            OrganizationBillingSetting setting,
            String packageName) {
        PlatformWalletSummaryDto dto = new PlatformWalletSummaryDto();
        dto.setOrganizationId(wallet.getOrganizationId());
        dto.setOrganizationName(wallet.getOrganizationName());
        dto.setBalanceCents(wallet.getBalanceCents());
        dto.setCurrencyCode(wallet.getCurrencyCode());
        if (setting != null) {
            dto.setBillingMode(setting.getBillingMode() != null ? setting.getBillingMode().name() : null);
            dto.setLowBalanceThresholdCents(setting.getLowBalanceThresholdCents());
            dto.setSubscriptionPackageId(setting.getSubscriptionPackageId());
            dto.setSubscriptionPackageName(packageName);
            long threshold = setting.getLowBalanceThresholdCents() != null ? setting.getLowBalanceThresholdCents() : 0L;
            long balance = wallet.getBalanceCents() != null ? wallet.getBalanceCents() : 0L;
            dto.setLowBalance(balance <= threshold);
            boolean prepaid = setting.getBillingMode() == OrganizationBillingMode.PREPAID_WALLET;
            boolean frozen = prepaid && balance <= 0;
            dto.setWalletFrozen(frozen);
            dto.setPlatformAccessAllowed(!frozen);
        }
        return dto;
    }

    public static WalletDepositDto toDto(WalletDeposit entity) {
        WalletDepositDto dto = new WalletDepositDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setAmountCents(entity.getAmountCents());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setReferenceNumber(entity.getReferenceNumber());
        dto.setNotes(entity.getNotes());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setProofDocumentId(entity.getProofDocumentId());
        dto.setGatewayProvider(entity.getGatewayProvider());
        dto.setPaymentMethod(entity.getPaymentMethod());
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(ISO));
        }
        if (entity.getModifiedAt() != null) {
            dto.setModifiedAt(entity.getModifiedAt().format(ISO));
        }
        dto.setModifiedBy(entity.getModifiedBy());
        return dto;
    }

    public static WalletTransactionDto toDto(WalletTransaction entity) {
        WalletTransactionDto dto = new WalletTransactionDto();
        dto.setId(entity.getId());
        dto.setTransactionType(entity.getTransactionType() != null ? entity.getTransactionType().name() : null);
        dto.setAmountCents(entity.getAmountCents());
        dto.setBalanceAfterCents(entity.getBalanceAfterCents());
        dto.setActionCode(entity.getActionCode());
        dto.setDescription(entity.getDescription());
        dto.setReceiptNumber(entity.getReceiptNumber());
        dto.setReceiptDocumentId(entity.getReceiptDocumentId());
        dto.setTripId(entity.getTripId());
        dto.setSeasonId(entity.getSeasonId());
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(ISO));
        }
        return dto;
    }

    public static UsageChargeRecordDto toDto(UsageChargeRecord entity) {
        UsageChargeRecordDto dto = new UsageChargeRecordDto();
        dto.setId(entity.getId());
        dto.setBillingMode(entity.getBillingMode() != null ? entity.getBillingMode().name() : null);
        dto.setActionCode(entity.getActionCode());
        dto.setActionDisplayName(entity.getActionDisplayName());
        dto.setChargeCents(entity.getChargeCents());
        dto.setDeducted(entity.getDeducted());
        dto.setTripId(entity.getTripId());
        dto.setSeasonId(entity.getSeasonId());
        dto.setReferenceType(entity.getReferenceType());
        dto.setReferenceId(entity.getReferenceId());
        dto.setServiceName(entity.getServiceName());
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().format(ISO));
        }
        return dto;
    }
}
