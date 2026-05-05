package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
public class LocationNodeDto {
    private Long id;
    private String name;
    private String code;
    private LocationType locationType;
    private Long parentId;
    private String parentName;
    private Long districtId;
    private String districtName;
    private Long suburbId;
    private String suburbName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private String postalCode;
    private List<String> aliases;
    private EntityStatus entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
