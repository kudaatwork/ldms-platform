package projectlx.co.zw.organizationmanagement.utils.dtos;

import projectlx.co.zw.organizationmanagement.model.TradingPartner;

import java.util.ArrayList;
import java.util.List;

public final class TradingPartnerMapping {

    private TradingPartnerMapping() {
    }

    public static TradingPartnerDto toDto(TradingPartner tp) {
        if (tp == null) {
            return null;
        }
        TradingPartnerDto dto = new TradingPartnerDto();
        dto.setId(tp.getId());
        if (tp.getOrganization() != null) {
            dto.setOrganizationId(tp.getOrganization().getId());
        }
        dto.setPartnerRole(tp.getPartnerRole());
        dto.setName(tp.getName());
        dto.setEmail(tp.getEmail());
        dto.setPhone(tp.getPhone());
        dto.setLocationId(tp.getLocationId());
        dto.setNotes(tp.getNotes());
        dto.setLinkedOrganizationId(tp.getLinkedOrganizationId());
        dto.setRecordOnly(tp.isRecordOnly());
        dto.setEntityStatus(tp.getEntityStatus());
        dto.setCreatedAt(tp.getCreatedAt());
        dto.setUpdatedAt(tp.getModifiedAt());
        return dto;
    }

    public static List<TradingPartnerDto> toDtos(List<TradingPartner> partners) {
        List<TradingPartnerDto> list = new ArrayList<>();
        if (partners == null) {
            return list;
        }
        for (TradingPartner tp : partners) {
            list.add(toDto(tp));
        }
        return list;
    }
}
