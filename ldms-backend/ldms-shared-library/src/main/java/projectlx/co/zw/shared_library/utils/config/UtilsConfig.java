package projectlx.co.zw.shared_library.utils.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.client.RestTemplate;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;
import projectlx.co.zw.shared_library.business.logic.impl.JwtServiceImpl;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.i18.impl.MessageServiceImpl;

@Configuration
public class UtilsConfig {

    @Bean(name = "customMessageSource")
    @Primary
    public MessageSource customMessageSource() {
        final ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public RestTemplate restTemplate () {
        return new RestTemplate();
    }
}
