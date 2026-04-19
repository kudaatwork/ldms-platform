package projectlx.co.zw.organizationmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.model.OrganizationKycReview;

import java.util.List;

public interface OrganizationKycReviewRepository extends JpaRepository<OrganizationKycReview, Long> {

    List<OrganizationKycReview> findByOrganizationOrderByReviewedAtDesc(Organization organization);
}
