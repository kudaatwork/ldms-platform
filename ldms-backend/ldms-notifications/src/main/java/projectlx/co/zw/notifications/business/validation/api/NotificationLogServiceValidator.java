package projectlx.co.zw.notifications.business.validation.api;

import java.util.Locale;
import projectlx.co.zw.notifications.utils.requests.NotificationLogMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

public interface NotificationLogServiceValidator {

    ValidatorDto isNotificationLogMultipleFiltersRequestValid(NotificationLogMultipleFiltersRequest request, Locale locale);
}
