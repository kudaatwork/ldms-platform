package projectlx.user.management.service.rest.backoffice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.user.management.service.processor.api.PlatformHealthServiceProcessor;
import projectlx.user.management.utils.responses.PlatformHealthResponse;

import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-user-management/v1/backoffice/platform-health")
@Tag(name = "Platform Health Backoffice Resource", description = "Platform-wide health and discovery snapshot")
@RequiredArgsConstructor
public class PlatformHealthBackofficeResource {

    private static final Logger logger = LoggerFactory.getLogger(PlatformHealthBackofficeResource.class);
    private final PlatformHealthServiceProcessor platformHealthServiceProcessor;

    @Auditable(action = "PLATFORM_HEALTH_SNAPSHOT")
    @PreAuthorize("hasAnyRole("
            + "T(projectlx.user.management.utils.security.PlatformRoles).ADMIN.toString(),"
            + "T(projectlx.user.management.utils.security.PlatformRoles).READ_ONLY.toString())")
    @GetMapping("/snapshot")
    @Operation(summary = "Platform health snapshot",
            description = "Probes configured LDMS microservice actuator health endpoints and aggregates infrastructure status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Snapshot generated"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
    public PlatformHealthResponse snapshot(
            @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
            @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
            final Locale locale) {
        logger.debug("Platform health snapshot requested");
        return platformHealthServiceProcessor.snapshot(locale);
    }
}
