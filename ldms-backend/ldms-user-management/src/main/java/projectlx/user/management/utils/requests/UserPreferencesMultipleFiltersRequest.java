package projectlx.user.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;

@Getter
@Setter
@ToString
public class UserPreferencesMultipleFiltersRequest extends MultipleFiltersRequest {
    private String preferredLanguage;
    private String timezone;
}
