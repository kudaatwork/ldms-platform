package projectlx.user.management.utils.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PlatformHealthSummaryDto {
    private int totalServices;
    private int upCount;
    private int downCount;
    private int unknownCount;
}
