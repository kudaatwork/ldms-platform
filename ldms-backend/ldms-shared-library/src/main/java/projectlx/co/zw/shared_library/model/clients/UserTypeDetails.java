package projectlx.co.zw.shared_library.model.clients;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserTypeDetails {
    private String userTypeName; // Name of the user type (e.g., ACCOUNTANT, FARMER, DRIVER)
    private String description; // Description of the user type
}
