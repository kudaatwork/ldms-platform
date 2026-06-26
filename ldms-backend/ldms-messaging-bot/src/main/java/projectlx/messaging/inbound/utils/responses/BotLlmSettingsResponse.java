package projectlx.messaging.inbound.utils.responses;

import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.messaging.inbound.utils.dtos.BotLlmSettingsDto;

@Getter
@Setter
public class BotLlmSettingsResponse extends CommonResponse {
    private BotLlmSettingsDto botLlmSettingsDto;
}
