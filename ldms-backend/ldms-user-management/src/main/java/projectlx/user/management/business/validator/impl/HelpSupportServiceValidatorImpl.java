package projectlx.user.management.business.validator.impl;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.business.validator.api.HelpSupportServiceValidator;
import projectlx.user.management.utils.requests.CreateSupportTicketRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HelpSupportServiceValidatorImpl implements HelpSupportServiceValidator {

    @Override
    public ValidatorDto isCreateSupportTicketRequestValid(CreateSupportTicketRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Request body is required.");
            return failure(errors);
        }
        if (request.getSubject() == null || request.getSubject().isBlank()) {
            errors.add("Subject is required.");
        } else if (request.getSubject().length() > 200) {
            errors.add("Subject must be at most 200 characters.");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            errors.add("Description is required.");
        } else if (request.getDescription().length() < 20) {
            errors.add("Description must be at least 20 characters.");
        } else if (request.getDescription().length() > 8000) {
            errors.add("Description must be at most 8000 characters.");
        }
        if (request.getCategory() == null) {
            errors.add("Category is required.");
        }
        return errors.isEmpty() ? success() : failure(errors);
    }

    private ValidatorDto success() {
        ValidatorDto dto = new ValidatorDto();
        dto.setSuccess(true);
        return dto;
    }

    private ValidatorDto failure(List<String> errors) {
        ValidatorDto dto = new ValidatorDto();
        dto.setSuccess(false);
        dto.setErrorMessages(errors);
        return dto;
    }
}
