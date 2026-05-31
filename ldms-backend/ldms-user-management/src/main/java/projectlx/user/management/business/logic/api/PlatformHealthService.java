package projectlx.user.management.business.logic.api;

import projectlx.user.management.utils.responses.PlatformHealthResponse;

import java.util.Locale;

public interface PlatformHealthService {
    PlatformHealthResponse snapshot(Locale locale);
}
