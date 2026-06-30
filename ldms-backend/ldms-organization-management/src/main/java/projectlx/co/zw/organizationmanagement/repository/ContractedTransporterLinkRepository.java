package projectlx.co.zw.organizationmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import projectlx.co.zw.organizationmanagement.model.ContractedTransporterLink;
import projectlx.co.zw.organizationmanagement.model.ContractedTransporterLinkId;
import projectlx.co.zw.organizationmanagement.utils.enums.TransporterLinkStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.List;
import java.util.Optional;

public interface ContractedTransporterLinkRepository
        extends JpaRepository<ContractedTransporterLink, ContractedTransporterLinkId> {

    Optional<ContractedTransporterLink> findByOrganizationIdAndTransporterId(Long organizationId, Long transporterId);

    List<ContractedTransporterLink> findByOrganizationIdAndEntityStatusNotOrderByLinkedAtDesc(
            Long organizationId, EntityStatus deleted);

    List<ContractedTransporterLink> findByTransporterIdAndEntityStatusNotOrderByLinkedAtDesc(
            Long transporterId, EntityStatus deleted);

    /** Pending offers awaiting this transporter's response. */
    List<ContractedTransporterLink> findByTransporterIdAndLinkStatusAndEntityStatusNotOrderByLinkedAtDesc(
            Long transporterId, TransporterLinkStatus linkStatus, EntityStatus deleted);
}
