package projectlx.fuel.expenses.utils.requests;

import lombok.Data;
import projectlx.fuel.expenses.utils.enums.FundRequestType;

import java.math.BigDecimal;

@Data
public class CreateFundRequestRequest {

    private Long tripId;
    private Long fleetDriverId;
    private Long fleetAssetId;

    /** FUEL_TOP_UP or FUNDS */
    private FundRequestType requestType;

    /** Populated for FUEL_TOP_UP requests */
    private BigDecimal litersRequested;

    /** Populated for FUNDS requests */
    private BigDecimal amountRequested;

    private String currencyCode;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String driverNotes;
}
