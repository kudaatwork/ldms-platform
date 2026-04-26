package projectlx.user.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for UserAddress.
 * Contains both the reference to the address in the User Management Service
 * and the details from the Location Service.
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressDto {

    private Long id;

    private Long locationAddressId;

    private String line1;
    private String line2;
    private String postalCode;
    private Long suburbId;
    private Long geoCoordinatesId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;
}
