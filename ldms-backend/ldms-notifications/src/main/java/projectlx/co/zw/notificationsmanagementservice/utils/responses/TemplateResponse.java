package projectlx.co.zw.notificationsmanagementservice.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.co.zw.notificationsmanagementservice.utils.dtos.NotificationTemplateDto;
import projectlx.co.zw.notificationsmanagementservice.utils.dtos.TemplateCreationMetadataDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateResponse extends CommonResponse {
    private NotificationTemplateDto template;
    private List<NotificationTemplateDto> templateList;
    private Page<NotificationTemplateDto> templatePage;
    /** Metadata for rendering the Add Template form (sections + channel options), same UX pattern as Add Organization. */
    private TemplateCreationMetadataDto addTemplateMetadata;
}
