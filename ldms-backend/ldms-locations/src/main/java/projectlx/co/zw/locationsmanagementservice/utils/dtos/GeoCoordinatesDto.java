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
public class GeoCoordinatesDto {

    private Long id;
    
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private EntityStatus entityStatus;
}