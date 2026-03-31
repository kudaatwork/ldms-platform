package projectlx.user.management.service.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import projectlx.user.management.service.model.EntityStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAccountDto {
    private Long id;
    private String phoneNumber;
    private String accountNumber;
    private Boolean isAccountLocked;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EntityStatus entityStatus;
}
