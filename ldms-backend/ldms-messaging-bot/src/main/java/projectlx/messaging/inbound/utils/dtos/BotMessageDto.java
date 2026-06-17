package projectlx.messaging.inbound.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotMessageDto {
    private String id;
    private String role;
    private String body;
    private String sentAt;
}
