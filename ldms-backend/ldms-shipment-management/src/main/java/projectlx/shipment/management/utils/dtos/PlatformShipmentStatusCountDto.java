package projectlx.shipment.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PlatformShipmentStatusCountDto {

    private String status;
    private String label;
    private long count;
    private String color;
}
