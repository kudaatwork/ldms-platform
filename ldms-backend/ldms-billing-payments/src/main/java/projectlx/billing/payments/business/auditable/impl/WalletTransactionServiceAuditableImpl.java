package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.WalletTransactionServiceAuditable;
import projectlx.billing.payments.model.WalletTransaction;
import projectlx.billing.payments.repository.WalletTransactionRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class WalletTransactionServiceAuditableImpl implements WalletTransactionServiceAuditable {

    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    public WalletTransaction create(WalletTransaction entity, Locale locale, String username) {
        return walletTransactionRepository.save(entity);
    }

    @Override
    public WalletTransaction update(WalletTransaction entity, Locale locale, String username) {
        return walletTransactionRepository.save(entity);
    }

    @Override
    public WalletTransaction delete(WalletTransaction entity, Locale locale) {
        return walletTransactionRepository.save(entity);
    }
}
