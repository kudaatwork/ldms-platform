package projectlx.billing.payments.utils.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.billing.payments.utils.dtos.PlatformRevenueReportDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

@Getter
@Setter
@ToString
public class PlatformRevenueReportResponse extends CommonResponse {

    private PlatformRevenueReportDto platformRevenueReportDto;
}
