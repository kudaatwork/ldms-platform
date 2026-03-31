package projectlx.co.zw.shared_library.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // geolocation & other shared metadata
    private Long locationId;

    private String assignedRegion;
    private String role;              // e.g. "Field Sales", "Account Manager"

    // represent/work‐for links
    private List<AgentOrganization> organizations = new ArrayList<>();

    // Branch relationship
    private Branch branch;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;
}
