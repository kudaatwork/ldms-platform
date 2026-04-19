package projectlx.co.zw.organizationmanagement.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.organizationmanagement.business.auditable.api.IndustryServiceAuditable;
import projectlx.co.zw.organizationmanagement.model.Industry;
import projectlx.co.zw.organizationmanagement.repository.IndustryRepository;

@RequiredArgsConstructor
public class IndustryServiceAuditableImpl implements IndustryServiceAuditable {

    private final IndustryRepository industryRepository;

    @Override
    public Industry save(Industry industry) {
        return industryRepository.save(industry);
    }
}
