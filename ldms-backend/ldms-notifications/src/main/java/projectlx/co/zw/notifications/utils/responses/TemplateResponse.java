package projectlx.co.zw.notifications.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;
import projectlx.co.zw.notifications.utils.dtos.NotificationTemplateDto;
import projectlx.co.zw.notifications.utils.dtos.TemplateCreationMetadataDto;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateResponse extends CommonResponse {

    private NotificationTemplateDto template;
    private List<NotificationTemplateDto> templateList;
    private Page<NotificationTemplateDto> templatePage;
    /** Metadata for rendering the Add Template form (sections + channel options), same UX pattern as Add Organization. */
    private TemplateCreationMetadataDto addTemplateMetadata;

    public NotificationTemplateDto getTemplate() {
        return template;
    }

    public void setTemplate(NotificationTemplateDto template) {
        this.template = template;
    }

    public List<NotificationTemplateDto> getTemplateList() {
        return templateList;
    }

    public void setTemplateList(List<NotificationTemplateDto> templateList) {
        this.templateList = templateList;
    }

    public Page<NotificationTemplateDto> getTemplatePage() {
        return templatePage;
    }

    public void setTemplatePage(Page<NotificationTemplateDto> templatePage) {
        this.templatePage = templatePage;
    }

    public TemplateCreationMetadataDto getAddTemplateMetadata() {
        return addTemplateMetadata;
    }

    public void setAddTemplateMetadata(TemplateCreationMetadataDto addTemplateMetadata) {
        this.addTemplateMetadata = addTemplateMetadata;
    }
}
