package projectlx.co.zw.organizationmanagement.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.organizationmanagement.business.auditable.api.OrganizationKycReviewServiceAuditable;
import projectlx.co.zw.organizationmanagement.model.OrganizationKycReview;
import projectlx.co.zw.organizationmanagement.repository.OrganizationKycReviewRepository;

@RequiredArgsConstructor
public class OrganizationKycReviewServiceAuditableImpl implements OrganizationKycReviewServiceAuditable {

    private final OrganizationKycReviewRepository organizationKycReviewRepository;

    @Override
    public OrganizationKycReview save(OrganizationKycReview review) {
        return organizationKycReviewRepository.save(review);
    }
}
