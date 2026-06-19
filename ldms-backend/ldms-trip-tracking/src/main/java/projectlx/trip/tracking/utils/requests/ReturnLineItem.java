package projectlx.trip.tracking.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class ReturnLineItem {

    private String productName;
    private BigDecimal quantity;
    private String reason;
}
