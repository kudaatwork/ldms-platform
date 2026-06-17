package projectlx.messaging.inbound.utils.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateBotSessionRequest {

    @NotBlank
    private String sessionId;

    /** 1 = poor, 5 = excellent */
    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;
}
