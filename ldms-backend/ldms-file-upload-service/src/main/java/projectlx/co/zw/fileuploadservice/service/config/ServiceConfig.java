package projectlx.co.zw.fileuploadservice.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.fileuploadservice.business.logic.api.FileUploadService;
import projectlx.co.zw.fileuploadservice.service.processor.api.FileUploadProcessor;
import projectlx.co.zw.fileuploadservice.service.processor.impl.FileUploadProcessorImpl;

@Configuration
public class ServiceConfig {

    @Bean
    public FileUploadProcessor fileUploadProcessor(FileUploadService fileUploadService) {
        return new FileUploadProcessorImpl(fileUploadService);
    }
}
