package projectlx.inventory.management.utils.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * Configures validators and other web-related beans
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Creates a LocalValidatorFactoryBean for JSR-303 Bean Validation
     * This is required for @Valid annotation to work properly with List<T> parameters
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * Enables method-level validation on beans
     * Required for @Valid to work on controller method parameters
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator());
        return processor;
    }
}
