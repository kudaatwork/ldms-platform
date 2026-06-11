package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.PlatformWalletServiceAuditable;
import projectlx.billing.payments.model.PlatformWallet;
import projectlx.billing.payments.repository.PlatformWalletRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class PlatformWalletServiceAuditableImpl implements PlatformWalletServiceAuditable {

    private final PlatformWalletRepository platformWalletRepository;

    @Override
    public PlatformWallet create(PlatformWallet entity, Locale locale, String username) {
        return platformWalletRepository.save(entity);
    }

    @Override
    public PlatformWallet update(PlatformWallet entity, Locale locale, String username) {
        return platformWalletRepository.save(entity);
    }

    @Override
    public PlatformWallet delete(PlatformWallet entity, Locale locale) {
        return platformWalletRepository.save(entity);
    }
}
