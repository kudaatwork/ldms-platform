package projectlx.co.zw.organizationmanagement.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.organizationmanagement.business.logic.api.OrganizationService;
import projectlx.co.zw.organizationmanagement.service.processor.api.OrganizationServiceProcessor;
import projectlx.co.zw.organizationmanagement.service.processor.impl.OrganizationServiceProcessorImpl;

@Configuration
public class ServiceConfig {

    @Bean
    public OrganizationServiceProcessor organizationServiceProcessor(OrganizationService organizationService) {
        return new OrganizationServiceProcessorImpl(organizationService);
    }
}
