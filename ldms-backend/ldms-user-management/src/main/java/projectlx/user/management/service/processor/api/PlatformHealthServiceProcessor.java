package projectlx.user.management.service.processor.api;

import projectlx.user.management.utils.responses.PlatformHealthResponse;

import java.util.Locale;

public interface PlatformHealthServiceProcessor {
    PlatformHealthResponse snapshot(Locale locale);
}
