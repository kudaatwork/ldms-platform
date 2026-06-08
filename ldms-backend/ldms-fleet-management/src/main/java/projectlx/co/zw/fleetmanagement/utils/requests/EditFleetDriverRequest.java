package projectlx.co.zw.fleetmanagement.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EditFleetDriverRequest {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String licenseNumber;
    private String licenseClass;
}
