package projectlx.user.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class HelpPlatformStatusDto {
    private String checkedAt;
    private String overallStatus;
    private String headline;
    private String detail;
    private Integer totalServices;
    private Integer upCount;
    private Integer downCount;
}
