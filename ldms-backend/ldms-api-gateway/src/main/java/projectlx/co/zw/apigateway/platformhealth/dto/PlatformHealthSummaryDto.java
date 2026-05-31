package projectlx.co.zw.apigateway.platformhealth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlatformHealthSummaryDto {
    private int totalServices;
    private int upCount;
    private int downCount;
    private int unknownCount;
}
