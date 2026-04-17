package projectlx.co.zw.shared_library.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

/**
 * Cross-module agent model (APIs, Feign DTOs, mapping). Not a JPA entity — persistence belongs in the
 * service that owns the {@code agent} table (e.g. user-management).
 */
@Getter
@Setter
@ToString
public class Agent {

    private Long id;
    private AgentKind agentKind;

    // — only for INDIVIDUAL —
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String nationalIdNumber;
    private String passportNumber;
    private LocalDateTime dateOfBirth;
    private Long agentUserId;

    private Organization organizationEntity;
    private Organization representedOrganization;

    private Long locationId;

    private String assignedRegion;
    private String role;

    private List<AgentOrganization> organizations = new ArrayList<>();

    private Branch branch;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;
}
