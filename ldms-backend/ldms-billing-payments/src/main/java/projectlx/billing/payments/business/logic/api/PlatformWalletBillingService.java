package projectlx.billing.payments.business.logic.api;

import projectlx.billing.payments.utils.requests.CreateWalletDepositRequest;
import projectlx.billing.payments.utils.requests.CreditOrganizationWalletRequest;
import projectlx.billing.payments.utils.requests.RecordPlatformUsageChargeRequest;
import projectlx.billing.payments.utils.requests.SaveOrganizationBillingSettingRequest;
import projectlx.billing.payments.utils.requests.SavePlatformActionChargeRequest;
import projectlx.billing.payments.utils.requests.SaveSubscriptionPackageRequest;
import projectlx.billing.payments.utils.requests.UsageChargeReportRequest;
import projectlx.billing.payments.utils.dtos.WalletReceiptPdfDto;
import projectlx.billing.payments.utils.responses.PlatformWalletResponse;

import java.util.Locale;

public interface PlatformWalletBillingService {

    PlatformWalletResponse getWalletSummary(Locale locale, String username);

    PlatformWalletResponse getWalletSummaryByOrganizationId(Long organizationId, Locale locale);

    PlatformWalletResponse getBillingSetting(Locale locale, String username);

    PlatformWalletResponse saveBillingSetting(SaveOrganizationBillingSettingRequest request, Locale locale, String username);

    PlatformWalletResponse listSubscriptionPackages(Locale locale, boolean activeOnly);

    PlatformWalletResponse createWalletDeposit(CreateWalletDepositRequest request, Locale locale, String username);

    PlatformWalletResponse listWalletDeposits(Locale locale, String username);

    PlatformWalletResponse listRecentWalletTransactions(Locale locale, String username);

    PlatformWalletResponse confirmWalletDeposit(Long depositId, Locale locale, String username);

    PlatformWalletResponse listPendingDeposits(Locale locale);

    PlatformWalletResponse listConfirmedDeposits(Locale locale);

    PlatformWalletResponse listActionCharges(Locale locale);

    PlatformWalletResponse getActionCharge(Long chargeId, Locale locale);

    PlatformWalletResponse listActiveActionCharges(Locale locale);

    PlatformWalletResponse saveActionCharge(SavePlatformActionChargeRequest request, Locale locale, String username);

    PlatformWalletResponse deleteActionCharge(Long chargeId, Locale locale, String username);

    PlatformWalletResponse saveSubscriptionPackage(SaveSubscriptionPackageRequest request, Locale locale, String username);

    PlatformWalletResponse deleteSubscriptionPackage(Long packageId, Locale locale, String username);

    PlatformWalletResponse getPublicPricingCatalog(Locale locale);

    PlatformWalletResponse recordUsageCharge(RecordPlatformUsageChargeRequest request, Locale locale, String actor);

    PlatformWalletResponse getUsageReport(UsageChargeReportRequest request, Locale locale, String username);

    PlatformWalletResponse creditOrganizationWallet(CreditOrganizationWalletRequest request, Locale locale, String username);

    PlatformWalletResponse getTransactionReceipt(Long transactionId, Locale locale, String username);

    WalletReceiptPdfDto getTransactionReceiptPdf(Long transactionId, Locale locale, String username);

    PlatformWalletResponse rejectWalletDeposit(Long depositId, Locale locale, String username);

    PlatformWalletResponse isFuelConsumptionAvailableForOrganization(Long organizationId, Locale locale);
}
