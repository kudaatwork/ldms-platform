package projectlx.user.authentication.service.model;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
public class UserType {

    private Long id;
    private String userTypeName;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<User> users;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;
}