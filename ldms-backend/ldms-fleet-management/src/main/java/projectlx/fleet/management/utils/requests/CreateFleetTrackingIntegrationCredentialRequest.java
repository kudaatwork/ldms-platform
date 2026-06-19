package projectlx.fleet.management.utils.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CreateFleetTrackingIntegrationCredentialRequest {

    @NotNull
    private Long organizationId;

    @NotBlank
    private String credentialLabel;

    @NotBlank
    private String integrationProvider;

    @NotNull
    private Long fleetAssetId;

    private String externalDeviceId;
    private String notes;
}
