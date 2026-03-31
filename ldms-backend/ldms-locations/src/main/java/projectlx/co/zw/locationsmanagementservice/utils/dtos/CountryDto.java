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
public class CountryDto {

    private Long id;
    
    private String name;
    private String isoAlpha2Code;
    private String isoAlpha3Code;
    private String dialCode;
    private String timezone;
    private String currencyCode;
    
    private Long geoCoordinatesId;
    private List<Long> localizedNameIds;
    private List<Long> administrativeLevelIds;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private EntityStatus entityStatus;
}