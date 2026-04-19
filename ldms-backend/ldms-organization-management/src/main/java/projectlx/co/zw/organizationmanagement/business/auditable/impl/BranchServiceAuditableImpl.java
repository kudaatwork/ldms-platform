package projectlx.co.zw.organizationmanagement.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.organizationmanagement.business.auditable.api.BranchServiceAuditable;
import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.repository.BranchRepository;

@RequiredArgsConstructor
public class BranchServiceAuditableImpl implements BranchServiceAuditable {

    private final BranchRepository branchRepository;

    @Override
    public Branch save(Branch branch) {
        return branchRepository.save(branch);
    }
}
