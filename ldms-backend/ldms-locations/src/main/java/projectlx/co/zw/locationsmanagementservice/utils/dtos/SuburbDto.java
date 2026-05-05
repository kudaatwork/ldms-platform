package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuburbDto {

    private Long id;
    
    private String name;
    private String code;
    
    private Long districtId;
    private String districtName;
    private Long provinceId;
    private String provinceName;
    private Long countryId;
    private String countryName;
    private Long geoCoordinatesId;
    private String postalCode;
    private Long administrativeLevelId;
    private String administrativeLevelName;
    private List<Long> localizedNameIds;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private EntityStatus entityStatus;
}