package projectlx.co.zw.shared_library.utils.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformBellNotificationRequest implements Serializable {

    private String eventId;
    private Long userId;
    private Long organizationId;
    private String eventKey;
    private String title;
    private String body;
    private String actionRoute;
    private String entityType;
    private Long entityId;
    private String sourceService;
}
