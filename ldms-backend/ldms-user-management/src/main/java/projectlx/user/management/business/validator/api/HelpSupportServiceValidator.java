package projectlx.user.management.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.utils.requests.CreateSupportTicketRequest;

import java.util.Locale;

public interface HelpSupportServiceValidator {
    ValidatorDto isCreateSupportTicketRequestValid(CreateSupportTicketRequest request, Locale locale);
}
