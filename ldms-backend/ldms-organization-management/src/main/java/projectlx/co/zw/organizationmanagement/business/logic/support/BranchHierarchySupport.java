package projectlx.co.zw.organizationmanagement.business.logic.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.repository.BranchRepository;
import projectlx.co.zw.organizationmanagement.utils.enums.BranchLevel;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BranchHierarchySupport {

    private final BranchRepository branchRepository;
    private final MessageService messageService;

    public Optional<String> validateHierarchyForCreate(Organization organization, Long parentBranchId, Locale locale) {
        if (parentBranchId == null || parentBranchId <= 0) {
            return Optional.empty();
        }
        Optional<Branch> parentOpt = branchRepository.findByIdAndEntityStatusNot(parentBranchId, EntityStatus.DELETED);
        if (parentOpt.isEmpty()) {
            return Optional.of(messageService.getMessage(I18Code.BRANCH_VALIDATION_FAILED.getCode(),
                    new String[]{"parent"}, locale));
        }
        Branch parent = parentOpt.get();
        if (!parent.getOrganization().getId().equals(organization.getId())) {
            return Optional.of(messageService.getMessage(I18Code.BRANCH_VALIDATION_FAILED.getCode(),
                    new String[]{"parent-org"}, locale));
        }
        if (parent.getBranchLevel() != BranchLevel.BRANCH) {
            return Optional.of(messageService.getMessage(I18Code.BRANCH_VALIDATION_FAILED.getCode(),
                    new String[]{"max-depth"}, locale));
        }
        return Optional.empty();
    }

    public Optional<String> validateHierarchyForUpdate(Branch branch, Long newParentBranchId, Locale locale) {
        if (newParentBranchId == null) {
            return Optional.empty();
        }
        if (newParentBranchId.equals(branch.getId())) {
            return Optional.of(messageService.getMessage(I18Code.BRANCH_VALIDATION_FAILED.getCode(),
                    new String[]{"self-parent"}, locale));
        }
        Optional<Branch> parentOpt = branchRepository.findByIdAndEntityStatusNot(newParentBranchId, EntityStatus.DELETED);
        if (parentOpt.isEmpty()) {
            return Optional.of(messageService.getMessage(I18Code.BRANCH_VALIDATION_FAILED.getCode(),
                    new String[]{"parent"}, locale));
        }
        Branch parent = parentOpt.get();
        if (!parent.getOrganization().getId().equals(branch.getOrganization().getId())) {
            return Optional.of(messageService.getMessage(I18Code.BRANCH_VALIDATION_FAILED.getCode(),
                    new String[]{"parent-org"}, locale));
        }
        if (parent.getBranchLevel() != BranchLevel.BRANCH) {
            return Optional.of(messageService.getMessage(I18Code.BRANCH_VALIDATION_FAILED.getCode(),
                    new String[]{"max-depth"}, locale));
        }
        if (branch.getBranchLevel() == BranchLevel.BRANCH && hasActiveSubBranches(branch.getId())) {
            return Optional.of(messageService.getMessage(I18Code.BRANCH_VALIDATION_FAILED.getCode(),
                    new String[]{"has-children"}, locale));
        }
        return Optional.empty();
    }

    public void applyHierarchyOnCreate(Branch branch, Organization organization, Long parentBranchId, Boolean depotFlag) {
        if (parentBranchId != null && parentBranchId > 0) {
            Branch parent = branchRepository.findByIdAndEntityStatusNot(parentBranchId, EntityStatus.DELETED)
                    .orElseThrow();
            branch.setParentBranch(parent);
            branch.setBranchLevel(BranchLevel.SUB_BRANCH);
            branch.setDepot(Boolean.TRUE.equals(depotFlag));
        } else {
            branch.setParentBranch(null);
            branch.setBranchLevel(BranchLevel.BRANCH);
            branch.setDepot(false);
        }
    }

    public void applyHierarchyOnUpdate(Branch branch, Long parentBranchId, Boolean depotFlag) {
        if (parentBranchId != null) {
            if (parentBranchId <= 0) {
                branch.setParentBranch(null);
                branch.setBranchLevel(BranchLevel.BRANCH);
                branch.setDepot(false);
            } else {
                Branch parent = branchRepository.findByIdAndEntityStatusNot(parentBranchId, EntityStatus.DELETED)
                        .orElseThrow();
                branch.setParentBranch(parent);
                branch.setBranchLevel(BranchLevel.SUB_BRANCH);
                if (depotFlag != null) {
                    branch.setDepot(depotFlag);
                }
            }
        } else if (depotFlag != null && branch.getBranchLevel() == BranchLevel.SUB_BRANCH) {
            branch.setDepot(depotFlag);
        }
    }

    private boolean hasActiveSubBranches(Long branchId) {
        return branchRepository.findByParentBranchIdAndEntityStatusNot(branchId, EntityStatus.DELETED).stream()
                .anyMatch(b -> b.getEntityStatus() != EntityStatus.DELETED);
    }
}
