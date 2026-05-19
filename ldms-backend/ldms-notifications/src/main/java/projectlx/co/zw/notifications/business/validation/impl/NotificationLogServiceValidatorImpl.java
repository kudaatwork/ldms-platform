package projectlx.co.zw.notifications.business.validation.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import projectlx.co.zw.notifications.business.validation.api.NotificationLogServiceValidator;
import projectlx.co.zw.notifications.utils.requests.NotificationLogMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

@RequiredArgsConstructor
public class NotificationLogServiceValidatorImpl implements NotificationLogServiceValidator {

    @Override
    public ValidatorDto isNotificationLogMultipleFiltersRequestValid(
            NotificationLogMultipleFiltersRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Filter request is required.");
            return new ValidatorDto(false, null, errors);
        }
        if (request.getPage() < 0) {
            errors.add("Page must be zero or greater.");
        }
        if (request.getSize() <= 0) {
            errors.add("Page size must be greater than zero.");
        }
        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        }
        return new ValidatorDto(false, null, errors);
    }
}
