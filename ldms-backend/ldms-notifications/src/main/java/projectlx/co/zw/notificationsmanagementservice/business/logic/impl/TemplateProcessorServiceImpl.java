package projectlx.co.zw.notificationsmanagementservice.business.logic.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.notificationsmanagementservice.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notificationsmanagementservice.business.validation.api.TemplateProcessorServiceValidator;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class TemplateProcessorServiceImpl implements TemplateProcessorService {

    private final TemplateProcessorServiceValidator validator;
    private final Handlebars handlebars = new Handlebars();
    // A simple cache to avoid re-compiling the same template repeatedly.
    private final Map<String, Template> templateCache = new ConcurrentHashMap<>();

    @Override
    public String process(String templateContent, Map<String, Object> data) {

        // Validate the template content and data
        ValidatorDto validationResult = validator.isValidForProcessing(templateContent, data, Locale.getDefault());

        if (!validationResult.getSuccess()) {
            log.warn("Template processing validation failed: {}", validationResult.getErrorMessages());
            return "";
        }

        try {

            Template template = templateCache.computeIfAbsent(templateContent, content -> {

                try {
                    return handlebars.compileInline(content);
                } catch (IOException e) {
                    log.error("Failed to compile Handlebars template", e);
                    throw new RuntimeException(e);
                }
            });

            return template.apply(data);

        } catch (IOException e) {
            log.error("Failed to apply data to Handlebars template", e);
            return templateContent; // Return original content on failure
        }
    }
}
