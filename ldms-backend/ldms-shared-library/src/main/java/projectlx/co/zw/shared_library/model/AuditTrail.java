package projectlx.co.zw.shared_library.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class AuditTrail {
    private Long id;
    private String actor;
    private String action;
    private LocalDateTime dateCreated;
    private String record;
}