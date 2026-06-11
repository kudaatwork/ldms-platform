package projectlx.fleet.management.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class CompleteFleetAssetRegistrationRequest {
    private List<FleetAssetRegistrationDocumentItem> documents;
}
