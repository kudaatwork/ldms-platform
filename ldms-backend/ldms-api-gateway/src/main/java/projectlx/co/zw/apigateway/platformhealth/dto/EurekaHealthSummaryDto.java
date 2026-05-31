package projectlx.co.zw.apigateway.platformhealth.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class EurekaHealthSummaryDto {
    private boolean available;
    private int registeredServiceCount;
    private int instanceCount;
    private List<EurekaServiceEntryDto> services = new ArrayList<>();

    @Getter
    @Setter
    public static class EurekaServiceEntryDto {
        private String serviceName;
        private int instanceCount;
    }
}
