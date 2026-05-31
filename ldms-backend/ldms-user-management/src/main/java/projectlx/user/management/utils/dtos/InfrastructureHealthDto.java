package projectlx.user.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InfrastructureHealthDto {
    private String component;
    private String status;
    private int servicesReporting;
    private int servicesUp;
    private int servicesDown;
}
