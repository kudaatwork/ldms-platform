package projectlx.fuel.expenses.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface FuelSessionServiceValidator {

    ValidatorDto isGetLiveByTripIdRequestValid(Long tripId, Locale locale);
}
