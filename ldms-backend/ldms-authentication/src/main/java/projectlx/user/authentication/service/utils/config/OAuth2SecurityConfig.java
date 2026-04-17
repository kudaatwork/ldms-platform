package projectlx.user.authentication.service.utils.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.authentication.service.utils.enums.I18Code;

@Configuration
@ConditionalOnProperty(prefix = "security.oauth2", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class OAuth2SecurityConfig {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @Bean
    @Order(0)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**", "/actuator/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",
                                        "/api/v1/system/**",
                                        "/api/v1/auth/**",
                                        "/actuator/**")
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .oauth2Login(oauth2 -> {})
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json;charset=UTF-8");
                    Locale locale = resolveLocale(request.getLocale());
                    String errorText = messageService.getMessage(
                            I18Code.MESSAGE_OAUTH2_UNAUTHORIZED.getCode(), new String[] {}, locale);
                    try {
                        objectMapper.writeValue(response.getOutputStream(), Map.of("error", errorText));
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to write OAuth2 unauthorized response", e);
                    }
                }));

        return http.build();
    }

    private static Locale resolveLocale(Locale requestLocale) {
        if (requestLocale != null && requestLocale.getLanguage() != null && !requestLocale.getLanguage().isEmpty()) {
            return requestLocale;
        }
        return Locale.ENGLISH;
    }
}
