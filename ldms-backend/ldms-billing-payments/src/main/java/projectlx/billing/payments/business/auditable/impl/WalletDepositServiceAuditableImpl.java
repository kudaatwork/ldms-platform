package projectlx.billing.payments.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.billing.payments.business.auditable.api.WalletDepositServiceAuditable;
import projectlx.billing.payments.model.WalletDeposit;
import projectlx.billing.payments.repository.WalletDepositRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class WalletDepositServiceAuditableImpl implements WalletDepositServiceAuditable {

    private final WalletDepositRepository walletDepositRepository;

    @Override
    public WalletDeposit create(WalletDeposit entity, Locale locale, String username) {
        return walletDepositRepository.save(entity);
    }

    @Override
    public WalletDeposit update(WalletDeposit entity, Locale locale, String username) {
        return walletDepositRepository.save(entity);
    }

    @Override
    public WalletDeposit delete(WalletDeposit entity, Locale locale) {
        return walletDepositRepository.save(entity);
    }
}
