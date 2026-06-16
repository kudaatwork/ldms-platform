package projectlx.inventory.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.responses.OrganizationResponse;
import projectlx.inventory.management.clients.OrganizationServiceClient;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BranchAllocationSupport {

    private final OrganizationServiceClient organizationServiceClient;

    public Optional<BranchDto> findBranch(Long branchId, Locale locale) {
        if (branchId == null || branchId <= 0) {
            return Optional.empty();
        }
        try {
            OrganizationResponse response = organizationServiceClient.getBranchById(branchId, locale);
            if (response != null && response.isSuccess() && response.getBranchDto() != null) {
                return Optional.of(response.getBranchDto());
            }
        } catch (Exception ex) {
            log.warn("Could not resolve branch {}: {}", branchId, ex.getMessage());
        }
        return Optional.empty();
    }

    public Optional<BranchDto> findHeadOfficeBranch(Long organizationId, Locale locale) {
        if (organizationId == null || organizationId <= 0) {
            return Optional.empty();
        }
        try {
            OrganizationResponse response = organizationServiceClient.getHeadOfficeBranch(organizationId, locale);
            if (response != null && response.isSuccess() && response.getBranchDto() != null) {
                return Optional.of(response.getBranchDto());
            }
        } catch (Exception ex) {
            log.warn("Could not resolve head-office branch for org {}: {}", organizationId, ex.getMessage());
        }
        return Optional.empty();
    }

    public Optional<String> validateBranchForOrganization(Long branchId, Long organizationId, Locale locale) {
        Optional<BranchDto> branchOpt = findBranch(branchId, locale);
        if (branchOpt.isEmpty()) {
            return Optional.of("Branch not found.");
        }
        BranchDto branch = branchOpt.get();
        if (branch.getOrganizationId() == null || !branch.getOrganizationId().equals(organizationId)) {
            return Optional.of("Branch does not belong to the organisation.");
        }
        return Optional.empty();
    }
}
