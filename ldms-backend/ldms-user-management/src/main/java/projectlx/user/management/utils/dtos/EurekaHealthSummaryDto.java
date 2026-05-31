package projectlx.user.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class EurekaHealthSummaryDto {
    private boolean available;
    private int registeredServiceCount;
    private int instanceCount;
    private List<EurekaServiceEntryDto> services = new ArrayList<>();

    @Getter
    @Setter
    @ToString
    public static class EurekaServiceEntryDto {
        private String serviceName;
        private int instanceCount;
    }
}
