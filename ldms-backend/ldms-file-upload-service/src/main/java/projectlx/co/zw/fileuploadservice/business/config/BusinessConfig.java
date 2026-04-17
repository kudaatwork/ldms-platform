package projectlx.co.zw.fileuploadservice.business.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.fileuploadservice.business.auditable.api.FileUploadServiceAuditable;
import projectlx.co.zw.fileuploadservice.business.auditable.impl.FileUploadServiceAuditableImpl;
import projectlx.co.zw.fileuploadservice.business.logic.api.FileUploadService;
import projectlx.co.zw.fileuploadservice.business.logic.impl.FileUploadServiceImpl;
import projectlx.co.zw.fileuploadservice.business.validator.api.FileUploadServiceValidator;
import projectlx.co.zw.fileuploadservice.business.validator.impl.FileUploadServiceValidatorImpl;
import projectlx.co.zw.fileuploadservice.repository.FileUploadRepository;
import projectlx.co.zw.fileuploadservice.storage.FileStorageService;
import projectlx.co.zw.fileuploadservice.storage.impl.LocalFileStorageServiceImpl;
import projectlx.co.zw.fileuploadservice.utils.config.FileUploadProperties;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.rustfs.RustFsClient;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class BusinessConfig {

    @Bean
    public FileStorageService localFileStorageService(FileUploadProperties fileUploadProperties) {
        return new LocalFileStorageServiceImpl(fileUploadProperties);
    }

    @Bean
    public FileUploadService fileUploadService(
            FileUploadServiceValidator fileUploadServiceValidator,
            FileUploadServiceAuditable fileUploadServiceAuditable,
            FileUploadRepository fileUploadRepository,
            RustFsClient rustFsClient,
            FileStorageService localFileStorageService,
            MessageService messageService,
            ModelMapper modelMapper,
            FileUploadProperties fileUploadProperties,
            ObjectMapper objectMapper) {
        return new FileUploadServiceImpl(
                fileUploadServiceValidator,
                fileUploadServiceAuditable,
                fileUploadRepository,
                rustFsClient,
                localFileStorageService,
                messageService,
                modelMapper,
                fileUploadProperties,
                objectMapper);
    }

    @Bean
    public FileUploadServiceValidator fileUploadServiceValidator(
            MessageService messageService, ObjectMapper objectMapper) {
        return new FileUploadServiceValidatorImpl(messageService, objectMapper);
    }

    @Bean
    public FileUploadServiceAuditable fileUploadServiceAuditable(FileUploadRepository fileUploadRepository) {
        return new FileUploadServiceAuditableImpl(fileUploadRepository);
    }
}
