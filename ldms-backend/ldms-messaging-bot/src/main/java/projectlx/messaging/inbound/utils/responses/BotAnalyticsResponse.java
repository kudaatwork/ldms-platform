package projectlx.messaging.inbound.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.messaging.inbound.utils.dtos.BotAnalyticsSummaryDto;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotAnalyticsResponse extends CommonResponse {
    private BotAnalyticsSummaryDto analyticsSummary;
}
