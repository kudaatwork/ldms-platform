package projectlx.billing.payments.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum I18Code {

    MESSAGE_REQUEST_NULL("billing.request.null"),
    MESSAGE_FIELD_REQUIRED("billing.field.required"),
    MESSAGE_ID_SUPPLIED_INVALID("billing.id.supplied.invalid"),
    MESSAGE_ORGANIZATION_UNRESOLVED("billing.organization.unresolved"),
    MESSAGE_CURRENCY_NOT_FOUND("billing.currency.not.found"),
    MESSAGE_CURRENCY_LIST_SUCCESS("billing.currency.list.success"),
    MESSAGE_CURRENCY_CREATE_SUCCESS("billing.currency.create.success"),
    MESSAGE_CURRENCY_CREATE_INVALID("billing.currency.create.invalid"),
    MESSAGE_COUNTRY_CURRENCY_LIST_SUCCESS("billing.country.currency.list.success"),
    MESSAGE_COUNTRY_CURRENCY_SAVE_SUCCESS("billing.country.currency.save.success"),
    MESSAGE_COUNTRY_CURRENCY_SAVE_INVALID("billing.country.currency.save.invalid"),
    MESSAGE_EXCHANGE_RATE_LIST_SUCCESS("billing.exchange.rate.list.success"),
    MESSAGE_EXCHANGE_RATE_CREATE_SUCCESS("billing.exchange.rate.create.success"),
    MESSAGE_EXCHANGE_RATE_CREATE_INVALID("billing.exchange.rate.create.invalid"),
    MESSAGE_EXCHANGE_RATE_NOT_FOUND("billing.exchange.rate.not.found"),
    MESSAGE_INVOICE_LIST_SUCCESS("billing.invoice.list.success"),
    MESSAGE_INVOICE_CREATE_SUCCESS("billing.invoice.create.success"),
    MESSAGE_PAYMENT_CREATE_SUCCESS("billing.payment.create.success"),
    MESSAGE_PAYMENT_CREATE_INVALID("billing.payment.create.invalid"),
    MESSAGE_INVOICE_NOT_FOUND("billing.invoice.not.found"),
    MESSAGE_DRIVER_EXPENSE_LIST_SUCCESS("billing.driver.expense.list.success"),
    MESSAGE_DRIVER_EXPENSE_APPROVE_SUCCESS("billing.driver.expense.approve.success"),
    MESSAGE_DRIVER_EXPENSE_REJECT_SUCCESS("billing.driver.expense.reject.success"),
    MESSAGE_DRIVER_EXPENSE_NOT_FOUND("billing.driver.expense.not.found"),
    MESSAGE_ORG_CURRENCY_LIST_SUCCESS("billing.org.currency.list.success"),
    MESSAGE_ORG_CURRENCY_SAVE_SUCCESS("billing.org.currency.save.success"),
    MESSAGE_ORG_CURRENCY_SAVE_INVALID("billing.org.currency.save.invalid"),
    MESSAGE_ORG_CURRENCY_CONTEXT_SUCCESS("billing.org.currency.context.success"),
    MESSAGE_PAYMENT_NOT_FOUND("billing.payment.not.found"),
    MESSAGE_PAYMENT_VERIFY_SUCCESS("billing.payment.verify.success"),
    MESSAGE_PAYMENT_VERIFY_INVALID("billing.payment.verify.invalid"),
    MESSAGE_PAYMENT_GATEWAY_UNSUPPORTED("billing.payment.gateway.unsupported"),
    MESSAGE_INVOICE_PO_CREATE_SUCCESS("billing.invoice.po.create.success"),

    MESSAGE_WALLET_SUMMARY_SUCCESS("billing.wallet.summary.success"),
    MESSAGE_BILLING_SETTING_SUCCESS("billing.setting.success"),
    MESSAGE_BILLING_SETTING_SAVE_SUCCESS("billing.setting.save.success"),
    MESSAGE_BILLING_SETTING_INVALID("billing.setting.invalid"),
    MESSAGE_SUBSCRIPTION_PACKAGE_LIST_SUCCESS("billing.subscription.package.list.success"),
    MESSAGE_SUBSCRIPTION_PACKAGE_SAVE_SUCCESS("billing.subscription.package.save.success"),
    MESSAGE_SUBSCRIPTION_PACKAGE_INVALID("billing.subscription.package.invalid"),
    MESSAGE_SUBSCRIPTION_PACKAGE_NOT_FOUND("billing.subscription.package.not.found"),
    MESSAGE_WALLET_DEPOSIT_CREATE_SUCCESS("billing.wallet.deposit.create.success"),
    MESSAGE_WALLET_DEPOSIT_LIST_SUCCESS("billing.wallet.deposit.list.success"),
    MESSAGE_WALLET_DEPOSIT_INVALID("billing.wallet.deposit.invalid"),
    MESSAGE_WALLET_DEPOSIT_NOT_FOUND("billing.wallet.deposit.not.found"),
    MESSAGE_WALLET_DEPOSIT_CONFIRM_SUCCESS("billing.wallet.deposit.confirm.success"),
    MESSAGE_WALLET_DEPOSIT_PREMIUM_ONLY("billing.wallet.deposit.premium.only"),
    MESSAGE_WALLET_TX_LIST_SUCCESS("billing.wallet.tx.list.success"),
    MESSAGE_WALLET_INSUFFICIENT_BALANCE("billing.wallet.insufficient.balance"),
    MESSAGE_ACTION_CHARGE_LIST_SUCCESS("billing.action.charge.list.success"),
    MESSAGE_ACTION_CHARGE_SAVE_SUCCESS("billing.action.charge.save.success"),
    MESSAGE_ACTION_CHARGE_INVALID("billing.action.charge.invalid"),
    MESSAGE_ACTION_CHARGE_NOT_FOUND("billing.action.charge.not.found"),
    MESSAGE_USAGE_CHARGE_INVALID("billing.usage.charge.invalid"),
    MESSAGE_USAGE_CHARGE_RECORDED("billing.usage.charge.recorded"),
    MESSAGE_USAGE_REPORT_SUCCESS("billing.usage.report.success");

    private final String code;
}
