package projectlx.billing.payments.business.logic.support;

import projectlx.billing.payments.model.CountryCurrencySetting;
import projectlx.billing.payments.model.Currency;
import projectlx.billing.payments.model.DriverExpenseReconciliation;
import projectlx.billing.payments.model.ExchangeRate;
import projectlx.billing.payments.model.Invoice;
import projectlx.billing.payments.model.InvoiceLine;
import projectlx.billing.payments.model.OrganizationCurrencySetting;
import projectlx.billing.payments.model.Payment;
import projectlx.billing.payments.utils.dtos.CountryCurrencySettingDto;
import projectlx.billing.payments.utils.dtos.CurrencyDto;
import projectlx.billing.payments.utils.dtos.DriverExpenseReconciliationDto;
import projectlx.billing.payments.utils.dtos.ExchangeRateDto;
import projectlx.billing.payments.utils.dtos.InvoiceDto;
import projectlx.billing.payments.utils.dtos.InvoiceLineDto;
import projectlx.billing.payments.utils.dtos.OrganizationCurrencySettingDto;
import projectlx.billing.payments.utils.dtos.PaymentDto;

public final class BillingMapper {

    private BillingMapper() {
    }

    public static CurrencyDto toDto(Currency entity) {
        CurrencyDto dto = new CurrencyDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setSymbol(entity.getSymbol());
        dto.setDecimalPlaces(entity.getDecimalPlaces());
        return dto;
    }

    public static CountryCurrencySettingDto toDto(CountryCurrencySetting entity) {
        CountryCurrencySettingDto dto = new CountryCurrencySettingDto();
        dto.setId(entity.getId());
        dto.setCountryId(entity.getCountryId());
        dto.setCountryName(entity.getCountryName());
        dto.setCountryIsoAlpha2(entity.getCountryIsoAlpha2());
        dto.setBaseCurrencyCode(entity.getBaseCurrencyCode());
        return dto;
    }

    public static ExchangeRateDto toDto(ExchangeRate entity) {
        ExchangeRateDto dto = new ExchangeRateDto();
        dto.setId(entity.getId());
        dto.setFromCurrencyCode(entity.getFromCurrencyCode());
        dto.setToCurrencyCode(entity.getToCurrencyCode());
        dto.setRate(entity.getRate());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setSource(entity.getSource());
        dto.setCurrent(entity.getEffectiveTo() == null);
        return dto;
    }

    public static OrganizationCurrencySettingDto toDto(OrganizationCurrencySetting entity, String countryDefaultCurrencyCode) {
        OrganizationCurrencySettingDto dto = new OrganizationCurrencySettingDto();
        dto.setId(entity.getId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setOrganizationName(entity.getOrganizationName());
        dto.setCountryId(entity.getCountryId());
        dto.setCountryIsoAlpha2(entity.getCountryIsoAlpha2());
        dto.setFunctionalCurrencyCode(entity.getFunctionalCurrencyCode());
        dto.setCountryDefaultCurrencyCode(countryDefaultCurrencyCode);
        return dto;
    }

    public static InvoiceDto toDto(Invoice entity) {
        InvoiceDto dto = new InvoiceDto();
        dto.setId(entity.getId());
        dto.setInvoiceNumber(entity.getInvoiceNumber());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setSupplierId(entity.getSupplierId());
        dto.setSourceType(entity.getSourceType());
        dto.setSourceId(entity.getSourceId());
        dto.setSourceReference(entity.getSourceReference());
        dto.setGrvId(entity.getGrvId());
        dto.setGrvNumber(entity.getGrvNumber());
        dto.setPurchaseOrderId(entity.getPurchaseOrderId());
        dto.setPurchaseOrderNumber(entity.getPurchaseOrderNumber());
        dto.setTransactionCurrencyCode(entity.getTransactionCurrencyCode());
        dto.setBaseCurrencyCode(entity.getBaseCurrencyCode());
        dto.setFunctionalCurrencyCode(entity.getBaseCurrencyCode());
        dto.setExchangeRateSnapshotId(entity.getExchangeRateSnapshotId());
        dto.setSubtotalTransaction(entity.getSubtotalTransaction());
        dto.setSubtotalBase(entity.getSubtotalBase());
        dto.setTaxTransaction(entity.getTaxTransaction());
        dto.setTaxBase(entity.getTaxBase());
        dto.setTotalTransaction(entity.getTotalTransaction());
        dto.setTotalBase(entity.getTotalBase());
        dto.setPaymentTerm(entity.getPaymentTerm());
        dto.setPaymentDueDate(entity.getPaymentDueDate());
        dto.setStatus(entity.getStatus());
        dto.setIssuedAt(entity.getIssuedAt());
        return dto;
    }

    public static InvoiceLineDto toDto(InvoiceLine entity) {
        InvoiceLineDto dto = new InvoiceLineDto();
        dto.setId(entity.getId());
        dto.setLineNumber(entity.getLineNumber());
        dto.setDescription(entity.getDescription());
        dto.setQuantity(entity.getQuantity());
        dto.setUnitPriceTransaction(entity.getUnitPriceTransaction());
        dto.setLineTotalTransaction(entity.getLineTotalTransaction());
        dto.setUnitPriceBase(entity.getUnitPriceBase());
        dto.setLineTotalBase(entity.getLineTotalBase());
        dto.setExchangeRateSnapshotId(entity.getExchangeRateSnapshotId());
        return dto;
    }

    public static PaymentDto toDto(Payment entity) {
        PaymentDto dto = new PaymentDto();
        dto.setId(entity.getId());
        dto.setPaymentReference(entity.getPaymentReference());
        dto.setInvoiceId(entity.getInvoiceId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setTransactionCurrencyCode(entity.getTransactionCurrencyCode());
        dto.setBaseCurrencyCode(entity.getBaseCurrencyCode());
        dto.setFunctionalCurrencyCode(entity.getBaseCurrencyCode());
        dto.setExchangeRateSnapshotId(entity.getExchangeRateSnapshotId());
        dto.setInvoiceExchangeRateSnapshotId(entity.getInvoiceExchangeRateSnapshotId());
        dto.setAmountTransaction(entity.getAmountTransaction());
        dto.setAmountBase(entity.getAmountBase());
        dto.setAmountFunctionalAtOrigination(entity.getAmountFunctionalAtOrigination());
        dto.setAmountFunctionalAtSettlement(entity.getAmountBase());
        dto.setRealizedFxGainLoss(entity.getRealizedFxGainLoss());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setPaymentDate(entity.getPaymentDate());
        dto.setStatus(entity.getStatus());
        dto.setNotes(entity.getNotes());
        dto.setPaymentReferenceNumber(entity.getPaymentReferenceNumber());
        dto.setProofDocumentId(entity.getProofDocumentId());
        dto.setProofSource(entity.getProofSource() != null ? entity.getProofSource().name() : null);
        dto.setGatewayProvider(entity.getGatewayProvider());
        dto.setVerifiedAt(entity.getVerifiedAt());
        dto.setVerifiedBy(entity.getVerifiedBy());
        dto.setCurrentVerificationStage(entity.getCurrentVerificationStage());
        dto.setRequiredVerificationStages(entity.getRequiredVerificationStages());
        return dto;
    }

    public static DriverExpenseReconciliationDto toDto(DriverExpenseReconciliation entity) {
        DriverExpenseReconciliationDto dto = new DriverExpenseReconciliationDto();
        dto.setId(entity.getId());
        dto.setExpenseReference(entity.getExpenseReference());
        dto.setDriverId(entity.getDriverId());
        dto.setOrganizationId(entity.getOrganizationId());
        dto.setTripId(entity.getTripId());
        dto.setTransactionCurrencyCode(entity.getTransactionCurrencyCode());
        dto.setBaseCurrencyCode(entity.getBaseCurrencyCode());
        dto.setExchangeRateSnapshotId(entity.getExchangeRateSnapshotId());
        dto.setAmountTransaction(entity.getAmountTransaction());
        dto.setAmountBase(entity.getAmountBase());
        dto.setExpenseCategory(entity.getExpenseCategory());
        dto.setExpenseDate(entity.getExpenseDate());
        dto.setStatus(entity.getStatus());
        dto.setApprovedBy(entity.getApprovedBy());
        dto.setApprovedAt(entity.getApprovedAt());
        dto.setRejectionReason(entity.getRejectionReason());
        return dto;
    }
}
