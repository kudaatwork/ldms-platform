package projectlx.co.zw.organizationmanagement.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.organizationmanagement.business.auditable.api.TradingPartnerServiceAuditable;
import projectlx.co.zw.organizationmanagement.model.TradingPartner;
import projectlx.co.zw.organizationmanagement.repository.TradingPartnerRepository;

@RequiredArgsConstructor
public class TradingPartnerServiceAuditableImpl implements TradingPartnerServiceAuditable {

    private final TradingPartnerRepository tradingPartnerRepository;

    @Override
    public TradingPartner save(TradingPartner tradingPartner) {
        return tradingPartnerRepository.save(tradingPartner);
    }
}
