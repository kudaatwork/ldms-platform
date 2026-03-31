package projectlx.co.zw.shared_library.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class Industry {
    private Long id;

    private String name; // Example: Agriculture, Finance, Technology

    private String industryCode; // Unique code e.g., "AGRI", "FIN", "TECH"

    private String description;

    // Optional Regulatory Details
    private String regulatoryBodyName;         // Example: "Ministry of Agriculture"
    private String regulatoryBodyContactInfo;  // Contact details or website

    private String complianceRequirements;    // Example: "Must be licensed by XYZ Authority"

    private boolean active = true;             // Enable/Disable industry without deleting

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private EntityStatus entityStatus;
}
