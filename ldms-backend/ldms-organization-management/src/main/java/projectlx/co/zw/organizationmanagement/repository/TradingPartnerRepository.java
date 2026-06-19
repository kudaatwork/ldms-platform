package projectlx.co.zw.organizationmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.model.TradingPartner;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.TradingPartnerRole;

import java.util.List;
import java.util.Optional;

public interface TradingPartnerRepository extends JpaRepository<TradingPartner, Long>,
        JpaSpecificationExecutor<TradingPartner> {

    List<TradingPartner> findByOrganizationAndEntityStatusNot(Organization organization, EntityStatus entityStatus);

    List<TradingPartner> findByOrganizationAndPartnerRoleAndEntityStatusNot(
            Organization organization, TradingPartnerRole partnerRole, EntityStatus entityStatus);

    Optional<TradingPartner> findByIdAndOrganizationAndEntityStatusNot(
            Long id, Organization organization, EntityStatus entityStatus);
}
