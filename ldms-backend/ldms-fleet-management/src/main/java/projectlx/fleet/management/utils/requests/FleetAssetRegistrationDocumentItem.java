package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class FleetAssetRegistrationDocumentItem {
    private String complianceType;
    private Long fileUploadId;
    private LocalDateTime expiresAt;
}
