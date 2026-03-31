package projectlx.user.authentication.service.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.user.authentication.service.business.logic.api.AuthenticationService;
import projectlx.user.authentication.service.service.processor.api.AuthenticationServiceProcessor;
import projectlx.user.authentication.service.service.processor.impl.AuthenticationServiceProcessorImpl;

@Configuration
public class ServiceConfig {
    @Bean
    public AuthenticationServiceProcessor authenticationServiceProcessor(AuthenticationService authenticationService) {
        return new AuthenticationServiceProcessorImpl(authenticationService);
    }
}
