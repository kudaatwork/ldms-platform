package projectlx.co.zw.organizationmanagement.utils.dtos;

import projectlx.co.zw.organizationmanagement.model.Industry;
import projectlx.co.zw.organizationmanagement.utils.requests.CreateIndustryRequest;
import projectlx.co.zw.organizationmanagement.utils.requests.UpdateIndustryRequest;

public final class IndustryMapping {

    private IndustryMapping() {
    }

    public static IndustryDto toDto(Industry industry) {
        IndustryDto dto = new IndustryDto();
        dto.setId(industry.getId());
        dto.setName(industry.getName());
        dto.setIndustryCode(industry.getIndustryCode());
        dto.setDescription(industry.getDescription());
        dto.setRegulatoryBodyName(industry.getRegulatoryBodyName());
        dto.setRegulatoryBodyContactInfo(industry.getRegulatoryBodyContactInfo());
        dto.setComplianceRequirements(industry.getComplianceRequirements());
        dto.setActive(industry.isActive());
        return dto;
    }

    public static IndustryUsageDto toUsageDto(Industry industry) {
        IndustryUsageDto dto = new IndustryUsageDto();
        dto.setId(industry.getId());
        dto.setName(industry.getName());
        dto.setIndustryCode(industry.getIndustryCode());
        dto.setDescription(industry.getDescription());
        dto.setRegulatoryBodyName(industry.getRegulatoryBodyName());
        dto.setRegulatoryBodyContactInfo(industry.getRegulatoryBodyContactInfo());
        dto.setComplianceRequirements(industry.getComplianceRequirements());
        dto.setActive(industry.isActive());
        return dto;
    }

    public static void applyCreate(CreateIndustryRequest request, Industry industry) {
        industry.setName(trim(request.getName()));
        industry.setIndustryCode(trim(request.getIndustryCode()));
        industry.setDescription(trim(request.getDescription()));
        industry.setRegulatoryBodyName(trim(request.getRegulatoryBodyName()));
        industry.setRegulatoryBodyContactInfo(trim(request.getRegulatoryBodyContactInfo()));
        industry.setComplianceRequirements(trim(request.getComplianceRequirements()));
        industry.setActive(request.getActive() == null || Boolean.TRUE.equals(request.getActive()));
    }

    public static void applyUpdate(UpdateIndustryRequest request, Industry industry) {
        industry.setName(trim(request.getName()));
        industry.setIndustryCode(trim(request.getIndustryCode()));
        industry.setDescription(trim(request.getDescription()));
        industry.setRegulatoryBodyName(trim(request.getRegulatoryBodyName()));
        industry.setRegulatoryBodyContactInfo(trim(request.getRegulatoryBodyContactInfo()));
        industry.setComplianceRequirements(trim(request.getComplianceRequirements()));
        if (request.getActive() != null) {
            industry.setActive(request.getActive());
        }
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
