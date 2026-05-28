package projectlx.user.authentication.service.service.rest;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import projectlx.user.authentication.service.utils.responses.AuthResponse;

/**
 * Maps unexpected auth failures to structured JSON instead of a bare HTTP 500 through the gateway.
 */
@Slf4j
@RestControllerAdvice(basePackageClasses = AuthenticationResource.class)
public class AuthenticationRestExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<AuthResponse> handleFeign(FeignException ex) {
        log.warn("Downstream call from authentication service failed: status={} message={}",
                ex.status(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "A required backend service is unavailable. Ensure user-management is running on port 8086."));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<AuthResponse> handleUserNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ex.getMessage() != null ? ex.getMessage() : "Invalid credentials"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> handleGeneral(Exception ex) {
        log.error("Unhandled error during authentication", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Authentication failed due to an internal error"));
    }

    private static AuthResponse errorResponse(int statusCode, String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(false);
        response.setStatusCode(statusCode);
        response.setMessage(message);
        return response;
    }
}
