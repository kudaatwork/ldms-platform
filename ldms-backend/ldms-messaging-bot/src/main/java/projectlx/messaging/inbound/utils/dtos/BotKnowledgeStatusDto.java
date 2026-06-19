package projectlx.messaging.inbound.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotKnowledgeStatusDto {
    private LocalDateTime lastLoadedAt;
    private int documentCount;
    private int characterCount;
    private int faqCount;
    private List<String> sources;
}
