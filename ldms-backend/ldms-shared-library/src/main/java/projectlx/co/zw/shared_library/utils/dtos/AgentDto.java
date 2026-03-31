package projectlx.co.zw.shared_library.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.model.Agent;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentDto {

    private Long id;

    // Name (personal identity only)
    private String firstName;
    private String lastName;

    // Contact (email and phone — use contact for "Contact" section in UI/API)
    private AgentContactDto contact;

    private String email;
    private String phoneNumber;
    private String nationalIdNumber;
    private String passportNumber;
    private LocalDateTime dateOfBirth;
    private Long agentUserId;

    // Address Details
    private Long locationId;

    // Uploaded Documents (represented as Base64 or URLs if needed)
    private byte[] nationalIdUploadUpload;
    private byte[] passportUploadUrl;

    private String nationalIdName;

    // Organization Representative Flag
    private Boolean isOrganization;

    // Organization Relations
    private Long representedOrganizationId;
    private Long organizationId;
    private Long branchId;

    // Additional Metadata
    private String assignedRegion;
    private String role;

    // Geolocation
    private Double latitude;
    private Double longitude;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;

    /**
     * Populates the contact (email, phoneNumber) on this DTO from the given entity.
     * Call after mapping Agent to AgentDto so that contact is set for API/UI "Contact" section.
     */
    public static void populateContact(Agent agent, AgentDto dto) {
        if (dto == null) return;
        AgentContactDto contact = new AgentContactDto();
        contact.setEmail(agent.getEmail());
        contact.setPhoneNumber(agent.getPhoneNumber());
        dto.setContact(contact);
    }

    public static void populateContact(Object agent, AgentDto dto) {
        if (dto == null || agent == null) return;
        try {
            AgentContactDto contact = new AgentContactDto();
            java.lang.reflect.Method getEmail = agent.getClass().getMethod("getEmail");
            java.lang.reflect.Method getPhoneNumber = agent.getClass().getMethod("getPhoneNumber");
            contact.setEmail((String) getEmail.invoke(agent));
            contact.setPhoneNumber((String) getPhoneNumber.invoke(agent));
            dto.setContact(contact);
        } catch (Exception e) {
            // Fallback or ignore
        }
    }

    /** Contact details (email, phone) — use this for "Contact" section in UI/API. */
    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AgentContactDto {
        private String email;
        private String phoneNumber;
    }
}
