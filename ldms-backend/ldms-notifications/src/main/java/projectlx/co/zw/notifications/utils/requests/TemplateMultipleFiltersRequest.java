package projectlx.co.zw.notifications.utils.requests;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.requests.MultipleFiltersRequest;
import java.util.List;

@Getter
@Setter
@ToString
public class TemplateMultipleFiltersRequest extends MultipleFiltersRequest {
    private String templateKey;
    private List<String> channels;
    private String inAppTitle;
    private String whatsappTemplateName;
    private boolean isActive;
}
