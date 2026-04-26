package projectlx.user.management.service.config;

import projectlx.user.management.business.logic.api.UserAccountService;
import projectlx.user.management.business.logic.api.UserAddressService;
import projectlx.user.management.business.logic.api.UserGroupService;
import projectlx.user.management.business.logic.api.UserPasswordService;
import projectlx.user.management.business.logic.api.UserPreferencesService;
import projectlx.user.management.business.logic.api.UserRoleService;
import projectlx.user.management.business.logic.api.UserSecurityService;
import projectlx.user.management.business.logic.api.UserService;
import projectlx.user.management.business.logic.api.UserTypeService;
import projectlx.user.management.service.processor.api.UserAccountServiceProcessor;
import projectlx.user.management.service.processor.api.UserAddressServiceProcessor;
import projectlx.user.management.service.processor.api.UserGroupServiceProcessor;
import projectlx.user.management.service.processor.api.UserPasswordServiceProcessor;
import projectlx.user.management.service.processor.api.UserPreferencesServiceProcessor;
import projectlx.user.management.service.processor.api.UserRoleServiceProcessor;
import projectlx.user.management.service.processor.api.UserSecurityServiceProcessor;
import projectlx.user.management.service.processor.api.UserServiceProcessor;
import projectlx.user.management.service.processor.api.UserTypeServiceProcessor;
import projectlx.user.management.service.processor.impl.UserAccountServiceProcessorImpl;
import projectlx.user.management.service.processor.impl.UserAddressServiceProcessorImpl;
import projectlx.user.management.service.processor.impl.UserGroupServiceProcessorImpl;
import projectlx.user.management.service.processor.impl.UserPasswordServiceProcessorImpl;
import projectlx.user.management.service.processor.impl.UserPreferencesServiceProcessorImpl;
import projectlx.user.management.service.processor.impl.UserRoleServiceProcessorImpl;
import projectlx.user.management.service.processor.impl.UserSecurityServiceProcessorImpl;
import projectlx.user.management.service.processor.impl.UserServiceProcessorImpl;
import projectlx.user.management.service.processor.impl.UserTypeServiceProcessorImpl;
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
