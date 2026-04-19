package projectlx.co.zw.organizationmanagement.utils.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class OrganizationPublicEndpointSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain organizationRegisterPermitAll(HttpSecurity http) throws Exception {
        http.securityMatcher(new AntPathRequestMatcher("/api/v1/frontend/organization/register", "POST"))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
