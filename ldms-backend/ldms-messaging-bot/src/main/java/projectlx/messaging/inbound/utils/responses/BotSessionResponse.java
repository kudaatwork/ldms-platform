package projectlx.messaging.inbound.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.messaging.inbound.utils.dtos.BotPricingDto;
import projectlx.messaging.inbound.utils.dtos.BotSessionDto;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotSessionResponse extends CommonResponse {
    private BotSessionDto botSessionDto;
    private List<BotSessionDto> botSessionDtoList;
    private BotPricingDto botPricingDto;
}
