package projectlx.user.management.business.config;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import projectlx.co.zw.shared_library.utils.config.UtilsConfig;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.logic.api.AuditTrailService;
import projectlx.user.management.business.logic.impl.AuditTrailServiceImpl;
import projectlx.user.management.business.auditable.api.UserAccountServiceAuditable;
import projectlx.user.management.business.auditable.api.UserAddressServiceAuditable;
import projectlx.user.management.business.auditable.api.UserGroupServiceAuditable;
import projectlx.user.management.business.auditable.api.UserPasswordServiceAuditable;
import projectlx.user.management.business.auditable.api.UserPreferencesServiceAuditable;
import projectlx.user.management.business.auditable.api.UserRoleServiceAuditable;
import projectlx.user.management.business.auditable.api.UserSecurityServiceAuditable;
import projectlx.user.management.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.business.auditable.api.UserTypeServiceAuditable;
import projectlx.user.management.business.auditable.impl.UserAccountServiceAuditableImpl;
import projectlx.user.management.business.auditable.impl.UserAddressServiceAuditableImpl;
import projectlx.user.management.business.auditable.impl.UserGroupServiceAuditableImpl;
import projectlx.user.management.business.auditable.impl.UserPasswordServiceAuditableImpl;
import projectlx.user.management.business.auditable.impl.UserPreferencesServiceAuditableImpl;
import projectlx.user.management.business.auditable.impl.UserRoleServiceAuditableImpl;
import projectlx.user.management.business.auditable.impl.UserSecurityServiceAuditableImpl;
import projectlx.user.management.business.auditable.impl.UserServiceAuditableImpl;
import projectlx.user.management.business.auditable.impl.UserTypeServiceAuditableImpl;
import projectlx.user.management.business.logic.api.UserAccountService;
import projectlx.user.management.business.logic.api.UserAddressService;
import projectlx.user.management.business.logic.api.UserGroupService;
import projectlx.user.management.business.logic.api.UserPasswordService;
import projectlx.user.management.business.logic.api.UserPreferencesService;
import projectlx.user.management.business.logic.api.UserRoleService;
import projectlx.user.management.business.logic.api.UserSecurityService;
import projectlx.user.management.business.logic.api.UserService;
import projectlx.user.management.business.logic.api.UserTypeService;
import projectlx.user.management.business.logic.impl.UserAccountServiceImpl;
import projectlx.user.management.business.logic.impl.UserAddressServiceImpl;
import projectlx.user.management.business.logic.impl.UserGroupServiceImpl;
import projectlx.user.management.business.logic.impl.UserPasswordServiceImpl;
import projectlx.user.management.business.logic.impl.UserPreferencesServiceImpl;
import projectlx.user.management.business.logic.impl.UserRoleServiceImpl;
import projectlx.user.management.business.logic.impl.UserSecurityServiceImpl;
import projectlx.user.management.business.logic.impl.UserServiceImpl;
import projectlx.user.management.business.logic.impl.UserTypeServiceImpl;
import projectlx.co.zw.shared_library.business.logic.impl.TokenService;
import projectlx.user.management.business.validator.api.UserAccountServiceValidator;
import projectlx.user.management.business.validator.api.UserAddressServiceValidator;
import projectlx.user.management.business.validator.api.UserGroupServiceValidator;
import projectlx.user.management.business.validator.api.UserPasswordServiceValidator;
import projectlx.user.management.business.validator.api.UserPreferencesServiceValidator;
import projectlx.user.management.business.validator.api.UserRoleServiceValidator;
import projectlx.user.management.business.validator.api.UserSecurityServiceValidator;
import projectlx.user.management.business.validator.api.UserServiceValidator;
import projectlx.user.management.business.validator.api.UserTypeServiceValidator;
import projectlx.user.management.business.validator.impl.UserAccountServiceValidatorImpl;
import projectlx.user.management.business.validator.impl.UserAddressServiceValidatorImpl;
import projectlx.user.management.business.validator.impl.UserGroupServiceValidatorImpl;
import projectlx.user.management.business.validator.impl.UserPasswordServiceValidatorImpl;
import projectlx.user.management.business.validator.impl.UserPreferencesServiceValidatorImpl;
import projectlx.user.management.business.validator.impl.UserRoleServiceValidatorImpl;
import projectlx.user.management.business.validator.impl.UserSecurityServiceValidatorImpl;
import projectlx.user.management.business.validator.impl.UserServiceValidatorImpl;
import projectlx.user.management.business.validator.impl.UserTypeServiceValidatorImpl;
import projectlx.user.management.clients.FileUploadServiceClient;
import projectlx.user.management.clients.LocationsServiceClient;
import projectlx.user.management.repository.UserAccountRepository;
import projectlx.user.management.repository.UserAddressRepository;
import projectlx.user.management.repository.UserGroupRepository;
import projectlx.user.management.repository.UserPasswordRepository;
import projectlx.user.management.repository.UserPreferencesRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.repository.UserRoleRepository;
import projectlx.user.management.repository.UserSecurityRepository;
import projectlx.user.management.repository.UserTypeRepository;
import org.modelmapper.ModelMapper;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
// Skip SharedDataConfig: stale JARs used @EntityScan(shared model) and can collide with local persistence mappings.
@Import({UtilsConfig.class})
public class BusinessConfig {

    @Bean
    public AuditTrailService auditTrailService(RabbitTemplate rabbitTemplate) {
        return new AuditTrailServiceImpl(rabbitTemplate);
    }

    @Bean
    public UserServiceAuditable userServiceAuditable(UserRepository userRepository){
        return new UserServiceAuditableImpl(userRepository);
    }

    @Bean
    public UserAccountServiceAuditable userAccountServiceAuditable(UserAccountRepository userAccountRepository){
        return new UserAccountServiceAuditableImpl(userAccountRepository) {
        };
    }

    @Bean
    public UserPasswordServiceAuditable userPasswordServiceAuditable(UserPasswordRepository userPasswordRepository){
        return new UserPasswordServiceAuditableImpl(userPasswordRepository) {
        };
    }

    @Bean
    public UserAddressServiceAuditable userAddressServiceAuditable(UserAddressRepository userAddressRepository){
        return new UserAddressServiceAuditableImpl(userAddressRepository) {
        };
    }

    @Bean
    public UserTypeServiceAuditable userTypeServiceAuditable(UserTypeRepository userTypeRepository){
        return new UserTypeServiceAuditableImpl(userTypeRepository) {
        };
    }

    @Bean
    public UserPreferencesServiceAuditable userPreferencesServiceAuditable(UserPreferencesRepository userPreferencesRepository){
        return new UserPreferencesServiceAuditableImpl(userPreferencesRepository) {
        };
    }

    @Bean
    public UserRoleServiceAuditable userRoleServiceAuditable(UserRoleRepository userRoleRepository){
        return new UserRoleServiceAuditableImpl(userRoleRepository) {
        };
    }

    @Bean
    public UserSecurityServiceAuditable userSecurityServiceAuditable(UserSecurityRepository userSecurityRepository){
        return new UserSecurityServiceAuditableImpl(userSecurityRepository) {
        };
    }

    @Bean
    public UserServiceValidator userServiceValidator(MessageService messageService){ return new UserServiceValidatorImpl(messageService); }

    @Bean
    public UserService userService(
            UserServiceValidator userServiceValidator, MessageService messageService, UserRepository userRepository,
            UserAccountRepository userAccountRepository, UserAddressRepository userAddressRepository,
            UserPasswordRepository userPasswordRepository, UserPreferencesRepository userPreferencesRepository,
            UserSecurityRepository userSecurityRepository, UserTypeRepository userTypeRepository,
            ModelMapper modelMapper, UserServiceAuditable userServiceAuditable, UserAccountServiceAuditable userAccountServiceAuditable,
            UserPasswordServiceAuditable userPasswordServiceAuditable, UserPreferencesServiceAuditable userPreferencesServiceAuditable,
            UserSecurityServiceAuditable userSecurityServiceAuditable, UserAccountService userAccountService,
            UserPasswordService userPasswordService, UserAddressService userAddressService,
            UserPreferencesService userPreferencesService, UserSecurityService userSecurityService, UserTypeService userTypeService,
            FileUploadServiceClient fileUploadServiceClient, RabbitTemplate rabbitTemplate, TokenService tokenService) {
        return new UserServiceImpl(userServiceValidator, messageService, userRepository, userAccountRepository,
                userAddressRepository, userPasswordRepository, userPreferencesRepository, userSecurityRepository,
                userTypeRepository, modelMapper, userServiceAuditable, userAccountServiceAuditable, userPasswordServiceAuditable,
                userPreferencesServiceAuditable, userSecurityServiceAuditable, userAccountService, userPasswordService,
                userAddressService, userPreferencesService, userSecurityService, userTypeService, fileUploadServiceClient,
                rabbitTemplate, tokenService
        );
    }

    @Bean
    public UserAccountService userAccountService(ModelMapper modelMapper, MessageService messageService, UserAccountRepository
                                                 userAccountRepository, UserRepository userRepository, UserAccountServiceAuditable
                                                 userAccountServiceAuditable, @Qualifier("userAccountServiceValidator") UserAccountServiceValidator
                                                 userAccountServiceValidator) {
        return new UserAccountServiceImpl(modelMapper, messageService, userAccountRepository, userRepository,
                userAccountServiceAuditable, userAccountServiceValidator);
    }

    @Bean
    public UserAddressServiceValidator userAddressServiceValidator(MessageService messageService){ return new UserAddressServiceValidatorImpl(messageService); }

    @Bean
    public UserGroupServiceValidator userGroupServiceValidator(MessageService messageService){ return new UserGroupServiceValidatorImpl(messageService); }

    @Bean
    public LocationsServiceClient locationsServiceClient(ApplicationContext context) {
        // Create a Feign client for LocationsServiceClient
        return new FeignClientBuilder(context)
                .forType(LocationsServiceClient.class, "locations-management-service")
                .url("${clients.base-url.locationService}")
                .build();
    }

    @Bean
    public UserAddressService userAddressService(UserAddressServiceValidator userAddressServiceValidator,
                                                 MessageService messageService, ModelMapper modelMapper,
                                                 UserAddressRepository userAddressRepository,
                                                 UserRepository userRepository, UserAddressServiceAuditable
                                                             userAddressServiceAuditable,
                                                 LocationsServiceClient locationsServiceClient) {
        return new UserAddressServiceImpl(userAddressServiceValidator, messageService, modelMapper, userAddressRepository,
                userRepository, userAddressServiceAuditable, locationsServiceClient);
    }

    @Bean
    public UserGroupServiceAuditable userGroupServiceAuditable(UserGroupRepository userGroupRepository) {
        return new UserGroupServiceAuditableImpl(userGroupRepository); }

    @Bean
    public UserGroupService userGroupService(UserGroupServiceValidator userGroupServiceValidator,
                                             MessageService messageService, ModelMapper modelMapper,
                                             UserGroupRepository userGroupRepository,
                                             UserRepository userRepository, UserGroupServiceAuditable userGroupServiceAuditable,
                                             UserRoleRepository userRoleRepository, UserServiceAuditable userServiceAuditable) {
        return new UserGroupServiceImpl(userGroupServiceValidator, messageService, modelMapper, userGroupRepository,
                userRepository, userGroupServiceAuditable, userRoleRepository, userServiceAuditable);
    }

    @Bean
    public UserPreferencesServiceValidator userPreferencesServiceValidator(MessageService messageService){ return new UserPreferencesServiceValidatorImpl(messageService); }

    @Bean
    public UserPreferencesService userPreferencesService(UserPreferencesServiceValidator userPreferencesServiceValidator,
                                                         MessageService messageService,
                                                         UserPreferencesRepository userPreferencesRepository,
                                                         UserRepository userRepository,
                                                         ModelMapper modelMapper, UserPreferencesServiceAuditable
                                                                     userPreferencesServiceAuditable) {
        return new UserPreferencesServiceImpl(userPreferencesServiceValidator, messageService, userPreferencesRepository,
               userRepository, modelMapper, userPreferencesServiceAuditable);
    }

    @Bean
    public UserTypeServiceValidator userTypeServiceValidator(MessageService messageService){ return new UserTypeServiceValidatorImpl(messageService); }

    @Bean
    public UserTypeService userTypeService(UserTypeServiceValidator userTypeServiceValidator, MessageService messageService,
                                           ModelMapper modelMapper, UserTypeRepository userTypeRepository,
                                           UserRepository userRepository, UserTypeServiceAuditable userTypeServiceAuditable) {
        return new UserTypeServiceImpl(userTypeServiceValidator, messageService, modelMapper, userTypeRepository,
                userRepository, userTypeServiceAuditable);
    }

    @Bean
    public UserSecurityServiceValidator userSecurityServiceValidator(MessageService messageService){ return new UserSecurityServiceValidatorImpl(messageService); }

    @Bean
    public UserRoleServiceValidator userRoleServiceValidator(MessageService messageService){ return new UserRoleServiceValidatorImpl(messageService); }

    @Bean
    public UserRoleService userRoleService(UserRoleServiceValidator userRoleServiceValidator, MessageService messageService,
                                           ModelMapper modelMapper, UserRoleRepository userRoleRepository,
                                           UserRepository userRepository, UserRoleServiceAuditable userRoleServiceAuditable) {
        return new UserRoleServiceImpl(userRoleServiceValidator, messageService, modelMapper, userRoleRepository,
                userRepository, userRoleServiceAuditable);
    }

    @Bean
    public UserSecurityService userSecurityService(UserSecurityServiceValidator userSecurityServiceValidator,
                                                   MessageService messageService, UserSecurityRepository userSecurityRepository,
                                                   UserRepository userRepository, ModelMapper modelMapper,
                                                   UserSecurityServiceAuditable userSecurityServiceAuditable) {
        return new UserSecurityServiceImpl(userSecurityServiceValidator, messageService, userSecurityRepository,
                userRepository, modelMapper, userSecurityServiceAuditable);
    }

    @Bean
    public UserAccountServiceValidator userAccountServiceValidator(MessageService messageService){ return new UserAccountServiceValidatorImpl(messageService); }

    @Bean
    public UserPasswordService userPasswordService(MessageService messageService, ModelMapper modelMapper,
                                                   UserPasswordRepository userPasswordRepository, UserRepository
                                                               userRepository, UserPasswordServiceValidator
                                                   userPasswordServiceValidator, UserPasswordServiceAuditable
                                                   userPasswordServiceAuditable, BCryptPasswordEncoder bCryptPasswordEncoder,
                                                   UserServiceAuditable userServiceAuditable, RabbitTemplate rabbitTemplate) {
        return new UserPasswordServiceImpl(messageService, modelMapper, userPasswordRepository, userRepository,
                userPasswordServiceValidator, userPasswordServiceAuditable, bCryptPasswordEncoder,
                userServiceAuditable, rabbitTemplate);
    }

    @Bean
    public UserPasswordServiceValidator userPasswordServiceValidator(MessageService messageService){ return new UserPasswordServiceValidatorImpl(messageService); }


    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
