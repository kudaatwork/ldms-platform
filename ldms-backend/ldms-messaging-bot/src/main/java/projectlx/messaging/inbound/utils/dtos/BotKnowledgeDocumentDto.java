package projectlx.messaging.inbound.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BotKnowledgeDocumentDto {
    private Long id;
    private String title;
    private String originalFilename;
    private String contentType;
    private long fileSizeBytes;
    private boolean published;
    private long useCount;
    private int extractedTextLength;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
