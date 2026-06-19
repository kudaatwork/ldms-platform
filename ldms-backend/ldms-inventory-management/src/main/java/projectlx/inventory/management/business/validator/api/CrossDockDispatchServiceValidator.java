package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.DispatchIngestRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface CrossDockDispatchServiceValidator {
    ValidatorDto isDispatchIngestRequestValid(DispatchIngestRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
}
