package projectlx.co.zw.notifications.utils.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UpdateTemplateRequest {
    @NotNull(message = "Template ID cannot be null for an update")
    private Long id;
    private boolean isActive;
}
