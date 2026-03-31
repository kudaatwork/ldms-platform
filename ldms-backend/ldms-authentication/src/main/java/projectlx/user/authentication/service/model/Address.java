package projectlx.user.authentication.service.model;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.dtos.AddressDto;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
public class Address {

    private Long id;
    private Long locationAddressId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;
    private List<User> users;

    @Transient
    private AddressDto addressDetails; // Transient field to hold address details from Location Service
}