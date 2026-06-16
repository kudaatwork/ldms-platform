package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@ToString
public class FleetAssetRegistrationDocumentItem {
    private String complianceType;
    private Long fileUploadId;
    /** Optional calendar expiry from the portal date picker (yyyy-MM-dd). */
    private LocalDate expiresAt;
}
