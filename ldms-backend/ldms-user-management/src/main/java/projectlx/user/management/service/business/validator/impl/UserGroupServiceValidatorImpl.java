package projectlx.user.management.service.business.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.validator.api.UserGroupServiceValidator;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.AddUserToUserGroupRequest;
import projectlx.user.management.service.utils.requests.AssignUserRoleToUserGroupRequest;
import projectlx.user.management.service.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.service.utils.requests.EditUserGroupRequest;
import projectlx.user.management.service.utils.requests.RemoveUserRolesFromUserGroupRequest;
import projectlx.user.management.service.utils.requests.UserGroupMultipleFiltersRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserGroupServiceValidatorImpl implements UserGroupServiceValidator {
    private static Logger logger = LoggerFactory.getLogger(UserGroupServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateUserGroupRequestValid(CreateUserGroupRequest createUserGroupRequest, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (createUserGroupRequest == null) {
            logger.info("Validation failed: CreateUserGroupRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_GROUP_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (createUserGroupRequest.getName() == null || createUserGroupRequest.getName().isEmpty()) {
            logger.info("Validation failed: Group name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_GROUP_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (createUserGroupRequest.getDescription() == null || createUserGroupRequest.getDescription().isEmpty()) {
            logger.info("Validation failed: Group description is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_GROUP_DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (createUserGroupRequest.getName() != null && doesStringHaveDigit(createUserGroupRequest.getName())) {
            logger.info("Validation failed: Group name contains digits");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_GROUP_NAME_CONTAINS_DIGITS.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (id == null || id <= 0L) {
            logger.info("Validation failed: ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditUserGroupRequest editUserGroupRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (editUserGroupRequest == null) {
            logger.info("Validation failed: EditUserGroupRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_GROUP_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (editUserGroupRequest.getId() == null || editUserGroupRequest.getId() <= 0L) {
            logger.info("Validation failed: Group ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (editUserGroupRequest.getName() == null || editUserGroupRequest.getName().isEmpty()) {
            logger.info("Validation failed: Group name is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_GROUP_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (editUserGroupRequest.getDescription() == null || editUserGroupRequest.getDescription().isEmpty()) {
            logger.info("Validation failed: Group description is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_GROUP_DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        if (editUserGroupRequest.getName() != null && !doesStringHaveDigit(editUserGroupRequest.getName())) {
            logger.info("Validation failed: Group name does not contain digits for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_GROUP_NAME_DOES_NOT_CONTAIN_DIGITS.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(UserGroupMultipleFiltersRequest userGroupMultipleFiltersRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (userGroupMultipleFiltersRequest == null) {
            logger.info("Validation failed: UserGroupMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_GROUP_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (userGroupMultipleFiltersRequest.getPage() < 0) {
            logger.info("Validation failed: Page number is less than 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_GROUP_MULTIPLE_FILTERS_PAGE_LESS_THAN_ZERO.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isStringValid(String input, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (input == null || input.isEmpty()) {
            logger.info("Validation failed: String is null or empty");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USERNAME_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidToAssignUserRolesToUserGroup(AssignUserRoleToUserGroupRequest assignUserRoleToUserGroupRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if(assignUserRoleToUserGroupRequest == null) {
            logger.info("Validation failed: AssignUserRoleToUserGroupRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ASSIGN_USER_ROLE_TO_USER_GROUP_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if(assignUserRoleToUserGroupRequest.getUserGroupId() == null ||
                assignUserRoleToUserGroupRequest.getUserGroupId() < 1) {
            logger.info("Validation failed: User group ID is null or less than 1");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ASSIGN_USER_ROLE_TO_USER_GROUP_USER_GROUP_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if(assignUserRoleToUserGroupRequest.getUserRoleIds() == null ||
                assignUserRoleToUserGroupRequest.getUserRoleIds().isEmpty()) {
            logger.info("Validation failed: User role IDs list is null or empty");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ASSIGN_USER_ROLE_TO_USER_GROUP_USER_ROLE_IDS_INVALID.getCode(), new String[]{}, locale));
        } else {
            for (Long userRoleId: assignUserRoleToUserGroupRequest.getUserRoleIds()) {
                if (userRoleId < 1) {
                    logger.info("Validation failed: User role ID is less than 1");
                    errors.add(messageService.getMessage(I18Code.MESSAGE_ASSIGN_USER_ROLE_TO_USER_GROUP_USER_ROLE_ID_LESS_THAN_ONE.getCode(), new String[]{}, locale));
                    break;
                }
            }
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRemoveUserRolesFromUserGroup(RemoveUserRolesFromUserGroupRequest removeUserRolesFromUserGroupRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if(removeUserRolesFromUserGroupRequest == null) {
            logger.info("Validation failed: RemoveUserRolesFromUserGroupRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REMOVE_USER_ROLES_FROM_USER_GROUP_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if(removeUserRolesFromUserGroupRequest.getUserGroupId() == null ||
                removeUserRolesFromUserGroupRequest.getUserGroupId() < 1) {
            logger.info("Validation failed: User group ID is null or less than 1 for removal");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REMOVE_USER_ROLES_FROM_USER_GROUP_USER_GROUP_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if(removeUserRolesFromUserGroupRequest.getUserRoleIds() == null ||
                removeUserRolesFromUserGroupRequest.getUserRoleIds().isEmpty()) {
            logger.info("Validation failed: User role IDs list is null or empty for removal");
            errors.add(messageService.getMessage(I18Code.MESSAGE_REMOVE_USER_ROLES_FROM_USER_GROUP_USER_ROLE_IDS_INVALID.getCode(), new String[]{}, locale));
        } else {
            for (Long userRoleId: removeUserRolesFromUserGroupRequest.getUserRoleIds()) {
                if (userRoleId < 1) {
                    logger.info("Validation failed: User role ID is less than 1 for removal");
                    errors.add(messageService.getMessage(I18Code.MESSAGE_REMOVE_USER_ROLES_FROM_USER_GROUP_USER_ROLE_ID_LESS_THAN_ONE.getCode(), new String[]{}, locale));
                    break;
                }
            }
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToAddUserToUserGroup(AddUserToUserGroupRequest addUserToUserGroupRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if(addUserToUserGroupRequest == null) {
            logger.info("Validation failed: AddUserToUserGroupRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ADD_USER_TO_USER_GROUP_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if(addUserToUserGroupRequest.getUserGroupId() == null ||
                addUserToUserGroupRequest.getUserGroupId() < 1) {
            logger.info("Validation failed: User group ID is null or less than 1");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ADD_USER_TO_USER_GROUP_USER_GROUP_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if(addUserToUserGroupRequest.getUserId() == null ||
                addUserToUserGroupRequest.getUserId() < 1) {
            logger.info("Validation failed: User ID is null or less than 1");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ADD_USER_TO_USER_GROUP_USER_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    static boolean doesStringHaveDigit(String input) {

        for (char c : input.toCharArray()) {

            if (Character.isDigit(c)) {
                return true;
            }
        }

        return false;
    }
}
