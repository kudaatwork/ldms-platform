package projectlx.billing.payments.service.processor.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.logic.api.PlatformWalletBillingService;
import projectlx.billing.payments.service.processor.api.PlatformWalletBillingServiceProcessor;
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

@RequiredArgsConstructor
public class PlatformWalletBillingServiceProcessorImpl implements PlatformWalletBillingServiceProcessor {

    private final PlatformWalletBillingService platformWalletBillingService;

    @Override
    public PlatformWalletResponse getWalletSummary(Locale locale, String username) {
        return platformWalletBillingService.getWalletSummary(locale, username);
    }

    @Override
    public PlatformWalletResponse getWalletSummaryByOrganizationId(Long organizationId, Locale locale) {
        return platformWalletBillingService.getWalletSummaryByOrganizationId(organizationId, locale);
    }

    @Override
    public PlatformWalletResponse getBillingSetting(Locale locale, String username) {
        return platformWalletBillingService.getBillingSetting(locale, username);
    }

    @Override
    public PlatformWalletResponse saveBillingSetting(
            SaveOrganizationBillingSettingRequest request, Locale locale, String username) {
        return platformWalletBillingService.saveBillingSetting(request, locale, username);
    }

    @Override
    public PlatformWalletResponse listSubscriptionPackages(Locale locale, boolean activeOnly) {
        return platformWalletBillingService.listSubscriptionPackages(locale, activeOnly);
    }

    @Override
    public PlatformWalletResponse createWalletDeposit(CreateWalletDepositRequest request, Locale locale, String username) {
        return platformWalletBillingService.createWalletDeposit(request, locale, username);
    }

    @Override
    public PlatformWalletResponse listWalletDeposits(Locale locale, String username) {
        return platformWalletBillingService.listWalletDeposits(locale, username);
    }

    @Override
    public PlatformWalletResponse listRecentWalletTransactions(Locale locale, String username) {
        return platformWalletBillingService.listRecentWalletTransactions(locale, username);
    }

    @Override
    public PlatformWalletResponse confirmWalletDeposit(Long depositId, Locale locale, String username) {
        return platformWalletBillingService.confirmWalletDeposit(depositId, locale, username);
    }

    @Override
    public PlatformWalletResponse listPendingDeposits(Locale locale) {
        return platformWalletBillingService.listPendingDeposits(locale);
    }

    @Override
    public PlatformWalletResponse listConfirmedDeposits(Locale locale) {
        return platformWalletBillingService.listConfirmedDeposits(locale);
    }

    @Override
    public PlatformWalletResponse listActionCharges(Locale locale) {
        return platformWalletBillingService.listActionCharges(locale);
    }

    @Override
    public PlatformWalletResponse getActionCharge(Long chargeId, Locale locale) {
        return platformWalletBillingService.getActionCharge(chargeId, locale);
    }

    @Override
    public PlatformWalletResponse listActiveActionCharges(Locale locale) {
        return platformWalletBillingService.listActiveActionCharges(locale);
    }

    @Override
    public PlatformWalletResponse saveActionCharge(
            SavePlatformActionChargeRequest request, Locale locale, String username) {
        return platformWalletBillingService.saveActionCharge(request, locale, username);
    }

    @Override
    public PlatformWalletResponse deleteActionCharge(Long chargeId, Locale locale, String username) {
        return platformWalletBillingService.deleteActionCharge(chargeId, locale, username);
    }

    @Override
    public PlatformWalletResponse saveSubscriptionPackage(
            SaveSubscriptionPackageRequest request, Locale locale, String username) {
        return platformWalletBillingService.saveSubscriptionPackage(request, locale, username);
    }

    @Override
    public PlatformWalletResponse deleteSubscriptionPackage(Long packageId, Locale locale, String username) {
        return platformWalletBillingService.deleteSubscriptionPackage(packageId, locale, username);
    }

    @Override
    public PlatformWalletResponse getPublicPricingCatalog(Locale locale) {
        return platformWalletBillingService.getPublicPricingCatalog(locale);
    }

    @Override
    public PlatformWalletResponse recordUsageCharge(
            RecordPlatformUsageChargeRequest request, Locale locale, String actor) {
        return platformWalletBillingService.recordUsageCharge(request, locale, actor);
    }

    @Override
    public PlatformWalletResponse getUsageReport(UsageChargeReportRequest request, Locale locale, String username) {
        return platformWalletBillingService.getUsageReport(request, locale, username);
    }

    @Override
    public PlatformWalletResponse creditOrganizationWallet(
            CreditOrganizationWalletRequest request, Locale locale, String username) {
        return platformWalletBillingService.creditOrganizationWallet(request, locale, username);
    }

    @Override
    public PlatformWalletResponse getTransactionReceipt(Long transactionId, Locale locale, String username) {
        return platformWalletBillingService.getTransactionReceipt(transactionId, locale, username);
    }

    @Override
    public WalletReceiptPdfDto getTransactionReceiptPdf(Long transactionId, Locale locale, String username) {
        return platformWalletBillingService.getTransactionReceiptPdf(transactionId, locale, username);
    }

    @Override
    public PlatformWalletResponse getDepositReceipt(Long depositId, Locale locale) {
        return platformWalletBillingService.getDepositReceipt(depositId, locale);
    }

    @Override
    public WalletReceiptPdfDto getDepositReceiptPdf(Long depositId, Locale locale) {
        return platformWalletBillingService.getDepositReceiptPdf(depositId, locale);
    }

    @Override
    public PlatformWalletResponse resendDepositReceipt(Long depositId, Locale locale, String username) {
        return platformWalletBillingService.resendDepositReceipt(depositId, locale, username);
    }

    @Override
    public PlatformWalletResponse rejectWalletDeposit(Long depositId, String rejectionReason, Locale locale, String username) {
        return platformWalletBillingService.rejectWalletDeposit(depositId, rejectionReason, locale, username);
    }

    @Override
    public PlatformWalletResponse isFuelConsumptionAvailableForOrganization(Long organizationId, Locale locale) {
        return platformWalletBillingService.isFuelConsumptionAvailableForOrganization(organizationId, locale);
    }
}
