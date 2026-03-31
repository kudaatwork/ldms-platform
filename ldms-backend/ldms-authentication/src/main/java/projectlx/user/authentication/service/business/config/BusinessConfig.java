package projectlx.user.authentication.service.business.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import projectlx.co.zw.shared_library.business.logic.api.JwtService;
import projectlx.co.zw.shared_library.business.logic.impl.JwtServiceImpl;
import projectlx.co.zw.shared_library.utils.config.UtilsConfig;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.authentication.service.business.auditable.api.AuthenticationServiceAuditable;
import projectlx.user.authentication.service.business.auditable.impl.AuthenticationServiceAuditableImpl;
import projectlx.user.authentication.service.business.logic.impl.CustomUserDetailsServiceImpl;
import projectlx.user.authentication.service.business.logic.api.AuthenticationService;
import projectlx.user.authentication.service.business.logic.impl.AuthenticationServiceImpl;
import projectlx.user.authentication.service.business.validator.api.AuthenticationServiceValidator;
import projectlx.user.authentication.service.business.validator.impl.AuthenticationServiceValidatorImpl;
import projectlx.user.authentication.service.clients.UserManagementServiceClient;
import projectlx.user.authentication.service.repository.TokenRepository;
import projectlx.user.authentication.service.repository.config.DataConfig;

@Configuration
@Import({DataConfig.class, UtilsConfig.class})
public class BusinessConfig {

    @Bean
    public CustomUserDetailsServiceImpl customUserDetailsService(UserManagementServiceClient userManagementServiceClient,
                                                                 ModelMapper modelMapper) {
        return new CustomUserDetailsServiceImpl(userManagementServiceClient, modelMapper);
    }

    @Bean
    public AuthenticationServiceValidator authenticationServiceValidator(){ return new AuthenticationServiceValidatorImpl(); }

    @Bean
    public AuthenticationServiceAuditable authenticationServiceAuditable(TokenRepository tokenRepository){
        return new AuthenticationServiceAuditableImpl(tokenRepository);
    }

    @Bean
    public AuthenticationService authenticationService(AuthenticationServiceValidator authenticationServiceValidator,
            MessageService messageService, ModelMapper modelMapper,
            TokenRepository tokenRepository, AuthenticationServiceAuditable authenticationServiceAuditable,
            UserManagementServiceClient userManagementServiceClient,
            CustomUserDetailsServiceImpl userDetailsService,
            JwtService jwtService,
            AuthenticationManager authManager
    ) {
        return new AuthenticationServiceImpl(authenticationServiceValidator, messageService, modelMapper,
                tokenRepository, authenticationServiceAuditable, userManagementServiceClient, userDetailsService,
                jwtService, authManager
        );
    }
}
