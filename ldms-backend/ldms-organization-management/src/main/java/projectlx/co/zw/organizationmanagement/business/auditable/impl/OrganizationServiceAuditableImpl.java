package projectlx.co.zw.organizationmanagement.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.organizationmanagement.business.auditable.api.OrganizationServiceAuditable;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.repository.OrganizationRepository;

@RequiredArgsConstructor
public class OrganizationServiceAuditableImpl implements OrganizationServiceAuditable {

    private final OrganizationRepository organizationRepository;

    @Override
    public Organization save(Organization organization) {
        return organizationRepository.save(organization);
    }
}
