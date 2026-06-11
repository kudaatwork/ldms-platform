package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.WalletTransaction;

import java.util.Locale;

public interface WalletTransactionServiceAuditable {
    WalletTransaction create(WalletTransaction entity, Locale locale, String username);
    WalletTransaction update(WalletTransaction entity, Locale locale, String username);
    WalletTransaction delete(WalletTransaction entity, Locale locale);
}
