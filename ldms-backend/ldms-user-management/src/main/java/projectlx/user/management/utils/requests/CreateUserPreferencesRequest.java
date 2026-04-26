package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateUserPreferencesRequest {
    private String preferredLanguage;
    private String timezone;
    private Long userId;
}
