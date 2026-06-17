package projectlx.user.management.business.validator.impl;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.business.validator.api.HelpSupportServiceValidator;
import projectlx.user.management.utils.requests.AddSupportTicketMessageRequest;
import projectlx.user.management.utils.requests.AssignSupportTicketRequest;
import projectlx.user.management.utils.requests.CreateDemoRequisitionRequest;
import projectlx.user.management.utils.requests.CreateSupportTicketRequest;
import projectlx.user.management.utils.requests.UpdateDemoRequisitionStatusRequest;
import projectlx.user.management.utils.requests.UpdateSupportTicketStatusRequest;

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

    @Override
    public ValidatorDto isAddSupportTicketMessageRequestValid(AddSupportTicketMessageRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Request body is required.");
            return failure(errors);
        }
        if (request.getSupportTicketId() == null || request.getSupportTicketId() <= 0) {
            errors.add("Support ticket id is required.");
        }
        if (request.getBody() == null || request.getBody().isBlank()) {
            errors.add("Message body is required.");
        } else if (request.getBody().length() > 8000) {
            errors.add("Message must be at most 8000 characters.");
        }
        return errors.isEmpty() ? success() : failure(errors);
    }

    @Override
    public ValidatorDto isUpdateSupportTicketStatusRequestValid(UpdateSupportTicketStatusRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Request body is required.");
            return failure(errors);
        }
        if (request.getSupportTicketId() == null || request.getSupportTicketId() <= 0) {
            errors.add("Support ticket id is required.");
        }
        if (request.getStatus() == null) {
            errors.add("Status is required.");
        }
        return errors.isEmpty() ? success() : failure(errors);
    }

    @Override
    public ValidatorDto isAssignSupportTicketRequestValid(AssignSupportTicketRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Request body is required.");
            return failure(errors);
        }
        if (request.getSupportTicketId() == null || request.getSupportTicketId() <= 0) {
            errors.add("Support ticket id is required.");
        }
        return errors.isEmpty() ? success() : failure(errors);
    }

    @Override
    public ValidatorDto isCreateDemoRequisitionRequestValid(CreateDemoRequisitionRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Request body is required.");
            return failure(errors);
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            errors.add("Full name is required.");
        } else if (request.getFullName().length() > 200) {
            errors.add("Full name must be at most 200 characters.");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            errors.add("Email is required.");
        } else if (request.getEmail().length() > 254) {
            errors.add("Email must be at most 254 characters.");
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            errors.add("Phone is required.");
        } else if (request.getPhone().length() > 50) {
            errors.add("Phone must be at most 50 characters.");
        }
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            errors.add("Address is required.");
        } else if (request.getAddress().length() > 500) {
            errors.add("Address must be at most 500 characters.");
        }
        if (request.getDemoRequest() == null || request.getDemoRequest().isBlank()) {
            errors.add("Demo request details are required.");
        } else if (request.getDemoRequest().length() < 20) {
            errors.add("Demo request must be at least 20 characters.");
        } else if (request.getDemoRequest().length() > 4000) {
            errors.add("Demo request must be at most 4000 characters.");
        }
        return errors.isEmpty() ? success() : failure(errors);
    }

    @Override
    public ValidatorDto isUpdateDemoRequisitionStatusRequestValid(UpdateDemoRequisitionStatusRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Request body is required.");
            return failure(errors);
        }
        if (request.getDemoRequisitionId() == null || request.getDemoRequisitionId() <= 0) {
            errors.add("Demo requisition id is required.");
        }
        if (request.getStatus() == null) {
            errors.add("Status is required.");
        }
        if (request.getAdminNotes() != null && request.getAdminNotes().length() > 4000) {
            errors.add("Admin notes must be at most 4000 characters.");
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
