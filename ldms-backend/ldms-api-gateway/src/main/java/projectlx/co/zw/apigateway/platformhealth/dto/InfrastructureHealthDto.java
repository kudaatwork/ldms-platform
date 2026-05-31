package projectlx.co.zw.apigateway.platformhealth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InfrastructureHealthDto {
    private String component;
    private String status;
    private int servicesReporting;
    private int servicesUp;
    private int servicesDown;
}
