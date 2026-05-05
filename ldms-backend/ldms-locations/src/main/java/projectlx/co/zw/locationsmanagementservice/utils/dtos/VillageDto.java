package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VillageDto {

    private Long id;
    private String name;
    private String code;
    private Long cityId;
    private String cityName;
    private Long districtId;
    private String districtName;
    private Long provinceId;
    private String provinceName;
    private Long countryId;
    private String countryName;
    private Long suburbId;
    private String suburbName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private String postalCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String modifiedBy;
    private EntityStatus entityStatus;
}
