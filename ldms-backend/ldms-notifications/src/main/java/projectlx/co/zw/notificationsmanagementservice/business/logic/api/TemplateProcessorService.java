package projectlx.co.zw.notificationsmanagementservice.business.logic.api;

import java.util.Map;

public interface TemplateProcessorService {
    /**
     * Renders a template string by replacing placeholders with provided data.
     * @param templateContent The template string (e.g., "Hello {{name}}").
     * @param data A map of variables to inject into the template.
     * @return The rendered string.
     */
    String process(String templateContent, Map<String, Object> data);
}
