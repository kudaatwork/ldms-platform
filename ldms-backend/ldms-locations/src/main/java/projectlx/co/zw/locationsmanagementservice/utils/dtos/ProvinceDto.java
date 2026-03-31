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
public class ProvinceDto {

    private Long id;
    
    private String name;
    private String code;
    
    private Long countryId;
    private Long administrativeLevelId;
    private List<Long> localizedNameIds;
    private Long geoCoordinatesId;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private EntityStatus entityStatus;
}