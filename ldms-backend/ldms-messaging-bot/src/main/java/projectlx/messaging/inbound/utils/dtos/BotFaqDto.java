package projectlx.messaging.inbound.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotFaqDto {
    private Long id;
    private String question;
    private String answer;
    private String category;
    private String keywords;
    private boolean published;
    private long useCount;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
