package projectlx.billing.payments.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.billing.payments.utils.dtos.PlatformBillingDashboardDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformBillingDashboardResponse extends CommonResponse {

    private PlatformBillingDashboardDto platformBillingDashboardDto;
}
