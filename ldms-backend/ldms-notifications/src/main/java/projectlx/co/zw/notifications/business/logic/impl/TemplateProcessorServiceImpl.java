package projectlx.co.zw.notifications.business.logic.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.notifications.business.logic.api.TemplateProcessorService;
import projectlx.co.zw.notifications.business.validation.api.TemplateProcessorServiceValidator;
import projectlx.co.zw.notifications.utils.NotificationTemplateDataEnricher;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TemplateProcessorServiceImpl implements TemplateProcessorService {

    private static final Logger log = LoggerFactory.getLogger(TemplateProcessorServiceImpl.class);

    private final TemplateProcessorServiceValidator validator;
    private final Handlebars handlebars = new Handlebars();

    public TemplateProcessorServiceImpl(TemplateProcessorServiceValidator validator) {
        this.validator = validator;
    }
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

            return template.apply(NotificationTemplateDataEnricher.enrich(data));

        } catch (IOException e) {
            log.error("Failed to apply data to Handlebars template", e);
            return templateContent; // Return original content on failure
        }
    }
}
