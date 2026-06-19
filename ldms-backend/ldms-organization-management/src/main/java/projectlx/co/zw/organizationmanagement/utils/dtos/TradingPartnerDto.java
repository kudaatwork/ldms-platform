package projectlx.co.zw.organizationmanagement.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.TradingPartnerRole;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TradingPartnerDto {

    private Long id;
    private Long organizationId;
    private TradingPartnerRole partnerRole;
    private String name;
    private String email;
    private String phone;
    private Long locationId;
    private String notes;
    private Long linkedOrganizationId;
    private boolean recordOnly;
    private EntityStatus entityStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
