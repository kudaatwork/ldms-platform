package projectlx.user.management.service.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EditUserPreferencesRequest {
    private Long id;
    private String preferredLanguage;
    private String timezone;
    private Long userId;
}
