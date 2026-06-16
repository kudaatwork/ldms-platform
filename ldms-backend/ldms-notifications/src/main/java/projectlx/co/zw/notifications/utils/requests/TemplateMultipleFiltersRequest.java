package projectlx.co.zw.notifications.utils.requests;

import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;
import java.util.List;

public class TemplateMultipleFiltersRequest extends MultipleFiltersRequest {
    private String templateKey;
    private List<String> channels;
    private String inAppTitle;
    private String whatsappTemplateName;
    private Boolean isActive;

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

    public List<String> getChannels() { return channels; }
    public void setChannels(List<String> channels) { this.channels = channels; }

    public String getInAppTitle() { return inAppTitle; }
    public void setInAppTitle(String inAppTitle) { this.inAppTitle = inAppTitle; }

    public String getWhatsappTemplateName() { return whatsappTemplateName; }
    public void setWhatsappTemplateName(String whatsappTemplateName) { this.whatsappTemplateName = whatsappTemplateName; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    @Override
    public String toString() {
        return "TemplateMultipleFiltersRequest{templateKey='" + templateKey + "', channels=" + channels + "}";
    }
}
