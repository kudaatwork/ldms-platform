package projectlx.co.zw.organizationmanagement.utils.dtos;

import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;

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
            dto.setOrganizationType(projectlx.co.zw.shared_library.model.OrganizationType.valueOf(o.getOrganizationType().name()));
        }
        if (o.getOrganizationClassification() != null) {
            dto.setOrganizationClassification(
                    projectlx.co.zw.shared_library.model.OrganizationClassification.valueOf(
                            o.getOrganizationClassification().name()));
        }
        if (o.getIndustry() != null) {
            dto.setIndustryId(o.getIndustry().getId());
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
        dto.setLocationId(b.getLocationId());
        dto.setPhoneNumber(b.getPhoneNumber());
        dto.setEmail(b.getEmail());
        dto.setHeadOffice(b.isHeadOffice());
        if (b.getOrganization() != null) {
            dto.setOrganizationId(b.getOrganization().getId());
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
}
