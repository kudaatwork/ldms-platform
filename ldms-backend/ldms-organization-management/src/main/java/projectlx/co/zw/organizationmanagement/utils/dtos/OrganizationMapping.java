package projectlx.co.zw.organizationmanagement.utils.dtos;

import projectlx.co.zw.organizationmanagement.model.Agent;
import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.shared_library.utils.dtos.AgentDto;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class OrganizationMapping {

    private OrganizationMapping() {
    }

    public static OrganizationDto toDto(Organization o) {
        if (o == null) {
            return null;
        }
        OrganizationDto dto = new OrganizationDto();
        dto.setId(o.getId());
        dto.setName(o.getName());
        dto.setLocationId(o.getLocationId());
        dto.setEmail(o.getEmail());
        dto.setPhoneNumber(o.getPhoneNumber());
        if (o.getOrganizationType() != null) {
            try {
                dto.setOrganizationType(
                        projectlx.co.zw.shared_library.model.OrganizationType.valueOf(o.getOrganizationType().name()));
            } catch (IllegalArgumentException ignored) {
                dto.setOrganizationType(projectlx.co.zw.shared_library.model.OrganizationType.OTHER);
            }
        }
        if (o.getOrganizationClassification() != null) {
            try {
                dto.setOrganizationClassification(
                        projectlx.co.zw.shared_library.model.OrganizationClassification.valueOf(
                                o.getOrganizationClassification().name()));
            } catch (IllegalArgumentException ignored) {
                // Leave classification unset when legacy/unknown value is stored.
            }
        }
        if (o.getIndustry() != null) {
            try {
                dto.setIndustryId(o.getIndustry().getId());
                dto.setIndustryName(o.getIndustry().getName());
                dto.setIndustryCode(o.getIndustry().getIndustryCode());
            } catch (RuntimeException ignored) {
                // Industry association not initialized — skip denormalized industry fields.
            }
        }
        dto.setContactPersonFirstName(o.getContactPersonFirstName());
        dto.setContactPersonLastName(o.getContactPersonLastName());
        dto.setContactPersonEmail(o.getContactPersonEmail());
        dto.setContactPersonPhoneNumber(o.getContactPersonPhoneNumber());
        dto.setContactPersonPosition(o.getContactPersonPosition());
        dto.setContactPersonGender(o.getContactPersonGender());
        dto.setContactPersonNationalIdNumber(o.getContactPersonNationalIdNumber());
        dto.setContactPersonNationalIdUploadId(o.getContactPersonNationalIdUploadId());
        dto.setContactPersonPassportNumber(o.getContactPersonPassportNumber());
        dto.setContactPersonPassportUploadId(o.getContactPersonPassportUploadId());
        dto.setContactPersonDateOfBirth(o.getContactPersonDateOfBirth());
        dto.setContactPersonUserId(o.getContactPersonUserId());
        dto.setLogoUploadId(o.getLogoUploadId());
        dto.setRegistrationNumber(o.getRegistrationNumber());
        dto.setTaxNumber(o.getTaxNumber());
        dto.setRegistrationCertificateUploadId(o.getRegistrationCertificateUploadId());
        dto.setTaxClearanceCertificateUploadId(o.getTaxClearanceCertificateUploadId());
        dto.setBusinessLicenseUploadId(o.getBusinessLicenseUploadId());
        dto.setProofOfAddressUploadId(o.getProofOfAddressUploadId());
        dto.setRepresentativeNationalIdNumber(o.getRepresentativeNationalIdNumber());
        dto.setRepresentativePassportNumber(o.getRepresentativePassportNumber());
        dto.setIndustrySpecificLicenseUploadId(o.getIndustrySpecificLicenseUploadId());
        dto.setWebsiteUrl(o.getWebsiteUrl());
        dto.setOrganizationDescription(o.getOrganizationDescription());
        dto.setIncorporationDate(o.getIncorporationDate());
        dto.setBusinessHours(o.getBusinessHours());
        dto.setNumberOfEmployees(o.getNumberOfEmployees());
        if (o.getAnnualRevenueEstimate() != null) {
            dto.setAnnualRevenueEstimate(o.getAnnualRevenueEstimate().doubleValue());
        }
        dto.setRegionsServed(o.getRegionsServed());
        dto.setSubscriptionPlanId(o.getSubscriptionPlanId());
        dto.setDataProtectionOfficerContact(o.getDataProtectionOfficerContact());
        dto.setTwoFactorAuthenticationEnabled(o.isTwoFactorAuthenticationEnabled());
        dto.setLinkedInUrl(o.getLinkedInUrl());
        dto.setFacebookUrl(o.getFacebookUrl());
        dto.setTwitterUrl(o.getTwitterUrl());
        dto.setInstagramUrl(o.getInstagramUrl());
        dto.setYoutubeUrl(o.getYoutubeUrl());
        if (o.getLatitude() != null) {
            dto.setLatitude(o.getLatitude().doubleValue());
        }
        if (o.getLongitude() != null) {
            dto.setLongitude(o.getLongitude().doubleValue());
        }
        dto.setAssignedAccountManagerUserId(o.getAssignedAccountManagerUserId());
        dto.setAssignedStage1ApproverUserId(o.getAssignedStage1ApproverUserId());
        dto.setAssignedStage1ApproverUsername(o.getAssignedStage1ApproverUsername());
        dto.setAssignedStage1ApproverDisplayName(o.getAssignedStage1ApproverUsername());
        dto.setAssignedStage2ApproverUserId(o.getAssignedStage2ApproverUserId());
        dto.setAssignedStage2ApproverUsername(o.getAssignedStage2ApproverUsername());
        dto.setAssignedStage2ApproverDisplayName(o.getAssignedStage2ApproverUsername());
        dto.setAssignedStage3ApproverUserId(o.getAssignedStage3ApproverUserId());
        dto.setAssignedStage3ApproverUsername(o.getAssignedStage3ApproverUsername());
        dto.setAssignedStage3ApproverDisplayName(o.getAssignedStage3ApproverUsername());
        dto.setAssignedStage4ApproverUserId(o.getAssignedStage4ApproverUserId());
        dto.setAssignedStage4ApproverUsername(o.getAssignedStage4ApproverUsername());
        dto.setAssignedStage4ApproverDisplayName(o.getAssignedStage4ApproverUsername());
        dto.setAssignedStage5ApproverUserId(o.getAssignedStage5ApproverUserId());
        dto.setAssignedStage5ApproverUsername(o.getAssignedStage5ApproverUsername());
        dto.setAssignedStage5ApproverDisplayName(o.getAssignedStage5ApproverUsername());
        dto.setKycRequiredApprovalStages(o.getKycRequiredApprovalStages());
        if (o.getKycStatus() != null) {
            dto.setKycStatus(o.getKycStatus().name());
        }
        dto.setSubmittedAt(o.getSubmittedAt());
        dto.setCurrentResubmissionCycle(o.getCurrentResubmissionCycle());
        dto.setStage1ReviewedBy(o.getStage1ReviewedBy());
        dto.setStage1ReviewedAt(o.getStage1ReviewedAt());
        dto.setStage2ReviewedBy(o.getStage2ReviewedBy());
        dto.setStage2ReviewedAt(o.getStage2ReviewedAt());
        dto.setLastRejectionReason(o.getLastRejectionReason());
        dto.setResubmissionCount(o.getResubmissionCount());
        dto.setIsVerified(o.isVerified());
        dto.setDuplexMode(o.isDuplexMode());
        dto.setStandaloneMode(o.isStandaloneMode());
        dto.setInventoryManagementEnabled(o.isInventoryManagementEnabled());
        dto.setCrossDockingEnabled(o.isCrossDockingEnabled());
        dto.setInventoryDataSource(o.getInventoryDataSource());
        dto.setCounterpartyEngagementMode(o.getCounterpartyEngagementMode());
        dto.setCreatedViaSignup(o.getCreatedViaSignup());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setUpdatedAt(o.getModifiedAt());
        dto.setEntityStatus(o.getEntityStatus());
        return dto;
    }

    public static BranchDto toBranchDto(Branch b) {
        BranchDto dto = new BranchDto();
        dto.setId(b.getId());
        dto.setBranchName(b.getBranchName());
        dto.setBranchCode(b.getBranchCode());
        dto.setLocationId(b.getLocationId());
        dto.setPhoneNumber(b.getPhoneNumber());
        dto.setEmail(b.getEmail());
        dto.setHeadOffice(b.isHeadOffice());
        dto.setActive(b.isActive());
        dto.setBranchLevel(b.getBranchLevel() != null ? b.getBranchLevel().name() : null);
        dto.setDepot(b.isDepot());
        if (b.getParentBranch() != null) {
            dto.setParentBranchId(b.getParentBranch().getId());
            dto.setParentBranchName(b.getParentBranch().getBranchName());
        }
        if (b.getOrganization() != null) {
            dto.setOrganizationId(b.getOrganization().getId());
            dto.setOrganizationName(b.getOrganization().getName());
        }
        dto.setManagerUserId(b.getManagerUserId());
        if (b.getLatitude() != null) {
            dto.setLatitude(b.getLatitude().doubleValue());
        }
        if (b.getLongitude() != null) {
            dto.setLongitude(b.getLongitude().doubleValue());
        }
        dto.setBusinessHours(b.getBusinessHours());
        dto.setRegion(b.getRegion());
        applyOrganizationContactFallbacks(b, dto);
        dto.setEntityStatus(b.getEntityStatus());
        dto.setCreatedAt(b.getCreatedAt());
        dto.setUpdatedAt(b.getModifiedAt());
        return dto;
    }

    public static List<BranchDto> toBranchDtos(List<Branch> branches) {
        List<BranchDto> list = new ArrayList<>();
        if (branches == null) {
            return list;
        }
        for (Branch b : branches) {
            list.add(toBranchDto(b));
        }
        return list;
    }

    /**
     * Head-office branches created by {@code V14__ensure_head_office_branch_per_org} often have no contact
     * columns populated. Surface organisation-level contact metadata in API responses when branch fields are blank.
     */
    private static void applyOrganizationContactFallbacks(Branch branch, BranchDto dto) {
        if (!branch.isHeadOffice()) {
            return;
        }
        Organization org = branch.getOrganization();
        if (org == null) {
            return;
        }
        if (!StringUtils.hasText(dto.getEmail())) {
            dto.setEmail(firstNonBlank(org.getEmail(), org.getContactPersonEmail()));
        }
        if (!StringUtils.hasText(dto.getPhoneNumber())) {
            dto.setPhoneNumber(firstNonBlank(org.getPhoneNumber(), org.getContactPersonPhoneNumber()));
        }
        if (!StringUtils.hasText(dto.getRegion())) {
            dto.setRegion(resolveRegionFromOrganization(org));
        }
        if (!StringUtils.hasText(dto.getBusinessHours())) {
            dto.setBusinessHours(trimToNull(org.getBusinessHours()));
        }
    }

    private static String resolveRegionFromOrganization(Organization org) {
        if (!StringUtils.hasText(org.getRegionsServed())) {
            return null;
        }
        String raw = org.getRegionsServed().trim();
        int comma = raw.indexOf(',');
        return comma > 0 ? raw.substring(0, comma).trim() : raw;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    public static AgentDto toAgentDto(Agent agent) {
        if (agent == null) {
            return null;
        }
        AgentDto dto = new AgentDto();
        dto.setId(agent.getId());
        dto.setFirstName(agent.getFirstName());
        dto.setLastName(agent.getLastName());
        dto.setEmail(agent.getEmail());
        dto.setPhoneNumber(agent.getPhoneNumber());
        dto.setAgentUserId(agent.getUserId());
        dto.setLocationId(agent.getLocationId());
        dto.setAssignedRegion(agent.getAssignedRegion());
        dto.setRole(agent.getRole());
        dto.setActive(agent.isActive());
        if (agent.getAgentKind() != null) {
            dto.setAgentKind(agent.getAgentKind().name());
        }
        dto.setAgentType(agent.getAgentType());
        if (agent.getOrganization() != null) {
            dto.setOrganizationId(agent.getOrganization().getId());
            dto.setOrganizationName(agent.getOrganization().getName());
        }
        if (agent.getRepresentedOrganization() != null) {
            dto.setRepresentedOrganizationId(agent.getRepresentedOrganization().getId());
        }
        if (agent.getBranch() != null) {
            dto.setBranchId(agent.getBranch().getId());
        }
        dto.setEntityStatus(agent.getEntityStatus());
        dto.setCreatedAt(agent.getCreatedAt());
        dto.setUpdatedAt(agent.getModifiedAt());
        AgentDto.populateContact(agent, dto);
        return dto;
    }

    public static List<AgentDto> toAgentDtos(List<Agent> agents) {
        List<AgentDto> list = new ArrayList<>();
        if (agents == null) {
            return list;
        }
        for (Agent a : agents) {
            list.add(toAgentDto(a));
        }
        return list;
    }
}
