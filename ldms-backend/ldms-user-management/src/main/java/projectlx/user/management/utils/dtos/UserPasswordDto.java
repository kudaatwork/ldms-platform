package projectlx.user.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPasswordDto {
    private Long id;
    private String password;
    private String description;
    private LocalDateTime expiryDate;
    private Boolean isPasswordExpired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
