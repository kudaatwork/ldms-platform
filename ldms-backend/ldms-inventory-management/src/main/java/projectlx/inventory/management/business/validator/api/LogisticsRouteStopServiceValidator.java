package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.ReplaceRouteStopsRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface LogisticsRouteStopServiceValidator {
    ValidatorDto isReplaceRouteStopsRequestValid(ReplaceRouteStopsRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
}
