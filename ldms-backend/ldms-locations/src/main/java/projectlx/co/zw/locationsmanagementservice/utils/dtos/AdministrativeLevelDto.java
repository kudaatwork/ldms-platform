package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdministrativeLevelDto {

    private Long id;
    
    private String name;
    private String code;
    private Integer level;
    private String description;

    /** Resolved from {@code country.id} for API consumers (not persisted on this DTO). */
    private Long countryId;
    /** Resolved from {@code country.name} for display. */
    private String countryName;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private EntityStatus entityStatus;
}