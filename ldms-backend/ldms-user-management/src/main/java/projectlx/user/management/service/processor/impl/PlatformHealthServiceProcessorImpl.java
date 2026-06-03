package projectlx.user.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.user.management.business.logic.api.PlatformHealthService;
import projectlx.user.management.service.processor.api.PlatformHealthServiceProcessor;
import projectlx.user.management.utils.responses.PlatformHealthResponse;

import java.util.Locale;

@RequiredArgsConstructor
public class PlatformHealthServiceProcessorImpl implements PlatformHealthServiceProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PlatformHealthServiceProcessorImpl.class);
    private final PlatformHealthService platformHealthService;

    @Override
    public PlatformHealthResponse snapshot(Locale locale) {
        logger.info("Incoming request for platform health snapshot");
        PlatformHealthResponse response = platformHealthService.snapshot(locale);
        logger.info("Platform health snapshot overallStatus={} up={}/{}",
                response.getOverallStatus(),
                response.getSummary() != null ? response.getSummary().getUpCount() : null,
                response.getSummary() != null ? response.getSummary().getTotalServices() : null);
        return response;
    }
}
