package projectlx.user.management.service.service.config;

import projectlx.user.management.service.business.logic.api.UserAccountService;
import projectlx.user.management.service.business.logic.api.UserAddressService;
import projectlx.user.management.service.business.logic.api.UserGroupService;
import projectlx.user.management.service.business.logic.api.UserPasswordService;
import projectlx.user.management.service.business.logic.api.UserPreferencesService;
import projectlx.user.management.service.business.logic.api.UserRoleService;
import projectlx.user.management.service.business.logic.api.UserSecurityService;
import projectlx.user.management.service.business.logic.api.UserService;
import projectlx.user.management.service.business.logic.api.UserTypeService;
import projectlx.user.management.service.service.processor.api.UserAccountServiceProcessor;
import projectlx.user.management.service.service.processor.api.UserAddressServiceProcessor;
import projectlx.user.management.service.service.processor.api.UserGroupServiceProcessor;
import projectlx.user.management.service.service.processor.api.UserPasswordServiceProcessor;
import projectlx.user.management.service.service.processor.api.UserPreferencesServiceProcessor;
import projectlx.user.management.service.service.processor.api.UserRoleServiceProcessor;
import projectlx.user.management.service.service.processor.api.UserSecurityServiceProcessor;
import projectlx.user.management.service.service.processor.api.UserServiceProcessor;
import projectlx.user.management.service.service.processor.api.UserTypeServiceProcessor;
import projectlx.user.management.service.service.processor.impl.UserAccountServiceProcessorImpl;
import projectlx.user.management.service.service.processor.impl.UserAddressServiceProcessorImpl;
import projectlx.user.management.service.service.processor.impl.UserGroupServiceProcessorImpl;
import projectlx.user.management.service.service.processor.impl.UserPasswordServiceProcessorImpl;
import projectlx.user.management.service.service.processor.impl.UserPreferencesServiceProcessorImpl;
import projectlx.user.management.service.service.processor.impl.UserRoleServiceProcessorImpl;
import projectlx.user.management.service.service.processor.impl.UserSecurityServiceProcessorImpl;
import projectlx.user.management.service.service.processor.impl.UserServiceProcessorImpl;
import projectlx.user.management.service.service.processor.impl.UserTypeServiceProcessorImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {
    @Bean
    public UserServiceProcessor userServiceProcessor(UserService userService) {
        return new UserServiceProcessorImpl(userService);
    }

    @Bean
    public UserAddressServiceProcessor userAddressServiceProcessor(UserAddressService userAddressService) {
        return new UserAddressServiceProcessorImpl(userAddressService);
    }

    @Bean
    public UserGroupServiceProcessor userGroupServiceProcessor(UserGroupService userGroupService) {
        return new UserGroupServiceProcessorImpl(userGroupService);
    }

    @Bean
    public UserAccountServiceProcessor userAccountServiceProcessor(UserAccountService userAccountService) {
        return new UserAccountServiceProcessorImpl(userAccountService);
    }

    @Bean
    public UserPreferencesServiceProcessor userPreferencesServiceProcessor(UserPreferencesService userPreferencesService) {
        return new UserPreferencesServiceProcessorImpl(userPreferencesService);
    }

    @Bean
    public UserPasswordServiceProcessor userPasswordServiceProcessor(UserPasswordService userPasswordService) {
        return new UserPasswordServiceProcessorImpl(userPasswordService);
    }

    @Bean
    public UserRoleServiceProcessor userRoleServiceProcessor(UserRoleService userRoleService) {
        return new UserRoleServiceProcessorImpl(userRoleService);
    }

    @Bean
    public UserSecurityServiceProcessor userSecurityServiceProcessor(UserSecurityService userSecurityService) {
        return new UserSecurityServiceProcessorImpl(userSecurityService);
    }

    @Bean
    UserTypeServiceProcessor userTypeServiceProcessor(UserTypeService userTypeService) {
        return new UserTypeServiceProcessorImpl(userTypeService);
    }
}
