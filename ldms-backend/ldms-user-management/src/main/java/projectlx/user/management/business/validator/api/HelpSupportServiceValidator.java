package projectlx.user.management.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.utils.requests.AddSupportTicketMessageRequest;
import projectlx.user.management.utils.requests.AssignSupportTicketRequest;
import projectlx.user.management.utils.requests.CreateDemoRequisitionRequest;
import projectlx.user.management.utils.requests.CreateSupportTicketRequest;
import projectlx.user.management.utils.requests.UpdateDemoRequisitionStatusRequest;
import projectlx.user.management.utils.requests.UpdateSupportTicketStatusRequest;

import java.util.Locale;

public interface HelpSupportServiceValidator {
    ValidatorDto isCreateSupportTicketRequestValid(CreateSupportTicketRequest request, Locale locale);

    ValidatorDto isAddSupportTicketMessageRequestValid(AddSupportTicketMessageRequest request, Locale locale);

    ValidatorDto isUpdateSupportTicketStatusRequestValid(UpdateSupportTicketStatusRequest request, Locale locale);

    ValidatorDto isAssignSupportTicketRequestValid(AssignSupportTicketRequest request, Locale locale);

    ValidatorDto isCreateDemoRequisitionRequestValid(CreateDemoRequisitionRequest request, Locale locale);

    ValidatorDto isUpdateDemoRequisitionStatusRequestValid(UpdateDemoRequisitionStatusRequest request, Locale locale);
}
