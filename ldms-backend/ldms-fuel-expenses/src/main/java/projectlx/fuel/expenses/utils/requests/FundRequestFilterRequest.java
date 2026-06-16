package projectlx.fuel.expenses.utils.requests;

import lombok.Data;
import projectlx.fuel.expenses.utils.enums.FundRequestStatus;
import projectlx.fuel.expenses.utils.enums.FundRequestType;

@Data
public class FundRequestFilterRequest {

    private Long tripId;
    private Long organizationId;
    private Long fleetDriverId;
    private Long fleetAssetId;
    private FundRequestStatus status;
    private FundRequestType requestType;

    private int page = 0;
    private int size = 20;
}
