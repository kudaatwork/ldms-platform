package projectlx.messaging.inbound.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.messaging.inbound.utils.dtos.BotKnowledgeDocumentDto;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotKnowledgeDocumentResponse extends CommonResponse {
    private BotKnowledgeDocumentDto botKnowledgeDocumentDto;
    private List<BotKnowledgeDocumentDto> botKnowledgeDocumentDtoList;
}
