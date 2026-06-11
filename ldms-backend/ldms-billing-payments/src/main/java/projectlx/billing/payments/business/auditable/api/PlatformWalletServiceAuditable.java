package projectlx.billing.payments.business.auditable.api;

import projectlx.billing.payments.model.PlatformWallet;

import java.util.Locale;

public interface PlatformWalletServiceAuditable {
    PlatformWallet create(PlatformWallet entity, Locale locale, String username);
    PlatformWallet update(PlatformWallet entity, Locale locale, String username);
    PlatformWallet delete(PlatformWallet entity, Locale locale);
}
