package projectlx.user.management.service.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPreferencesDto {
    private Long id;
    private String preferredLanguage;
    private String timezone;
}
