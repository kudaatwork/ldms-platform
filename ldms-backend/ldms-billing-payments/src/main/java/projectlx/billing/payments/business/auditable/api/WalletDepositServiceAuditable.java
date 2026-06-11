package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.WalletDeposit;

import java.util.Locale;

public interface WalletDepositServiceAuditable {
    WalletDeposit create(WalletDeposit entity, Locale locale, String username);
    WalletDeposit update(WalletDeposit entity, Locale locale, String username);
    WalletDeposit delete(WalletDeposit entity, Locale locale);
}
