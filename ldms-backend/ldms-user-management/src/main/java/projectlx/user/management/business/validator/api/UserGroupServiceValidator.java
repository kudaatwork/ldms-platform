package projectlx.user.management.business.validator.api;

import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.user.management.utils.requests.AddUserToUserGroupRequest;
import projectlx.user.management.utils.requests.AssignUserRoleToUserGroupRequest;
import projectlx.user.management.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.utils.requests.EditUserGroupRequest;
import projectlx.user.management.utils.requests.RemoveUserRolesFromUserGroupRequest;
import projectlx.user.management.utils.requests.UserGroupMultipleFiltersRequest;

import java.util.Locale;

public interface UserGroupServiceValidator {
    // Methods returning ValidatorDto with Locale parameter
    ValidatorDto isCreateUserGroupRequestValid(CreateUserGroupRequest createUserGroupRequest, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditUserGroupRequest editUserGroupRequest, Locale locale);
    ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(UserGroupMultipleFiltersRequest userGroupMultipleFiltersRequest, Locale locale);
    ValidatorDto isStringValid(String input, Locale locale);
    ValidatorDto isRequestValidToAssignUserRolesToUserGroup(AssignUserRoleToUserGroupRequest assignUserRoleToUserGroupRequest, Locale locale);
    ValidatorDto isRequestValidToRemoveUserRolesFromUserGroup(RemoveUserRolesFromUserGroupRequest removeUserRolesFromUserGroupRequest, Locale locale);
    ValidatorDto isRequestValidToAddUserToUserGroup(AddUserToUserGroupRequest addUserToUserGroupRequest, Locale locale);

    // Methods returning ValidatorDto without Locale parameter
    default ValidatorDto isCreateUserGroupRequestValidDto(CreateUserGroupRequest createUserGroupRequest) {
        return isCreateUserGroupRequestValid(createUserGroupRequest, Locale.getDefault());
    }

    default ValidatorDto isIdValidDto(Long id) {
        return isIdValid(id, Locale.getDefault());
    }

    default ValidatorDto isRequestValidForEditingDto(EditUserGroupRequest editUserGroupRequest) {
        return isRequestValidForEditing(editUserGroupRequest, Locale.getDefault());
    }

    default ValidatorDto isRequestValidToRetrieveUsersByMultipleFiltersDto(UserGroupMultipleFiltersRequest userGroupMultipleFiltersRequest) {
        return isRequestValidToRetrieveUsersByMultipleFilters(userGroupMultipleFiltersRequest, Locale.getDefault());
    }

    default ValidatorDto isStringValidDto(String input) {
        return isStringValid(input, Locale.getDefault());
    }

    default ValidatorDto isRequestValidToAssignUserRolesToUserGroupDto(AssignUserRoleToUserGroupRequest assignUserRoleToUserGroupRequest) {
        return isRequestValidToAssignUserRolesToUserGroup(assignUserRoleToUserGroupRequest, Locale.getDefault());
    }

    default ValidatorDto isRequestValidToRemoveUserRolesFromUserGroupDto(RemoveUserRolesFromUserGroupRequest removeUserRolesFromUserGroupRequest) {
        return isRequestValidToRemoveUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, Locale.getDefault());
    }

    default ValidatorDto isRequestValidToAddUserToUserGroupDto(AddUserToUserGroupRequest addUserToUserGroupRequest) {
        return isRequestValidToAddUserToUserGroup(addUserToUserGroupRequest, Locale.getDefault());
    }

    // Methods returning boolean for backward compatibility with tests
    default boolean isCreateUserGroupRequestValid(CreateUserGroupRequest createUserGroupRequest) {
        return isCreateUserGroupRequestValid(createUserGroupRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isIdValid(Long id) {
        return isIdValid(id, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidForEditing(EditUserGroupRequest editUserGroupRequest) {
        return isRequestValidForEditing(editUserGroupRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidToRetrieveUsersByMultipleFilters(UserGroupMultipleFiltersRequest userGroupMultipleFiltersRequest) {
        return isRequestValidToRetrieveUsersByMultipleFilters(userGroupMultipleFiltersRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isStringValid(String input) {
        return isStringValid(input, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidToAssignUserRolesToUserGroup(AssignUserRoleToUserGroupRequest assignUserRoleToUserGroupRequest) {
        return isRequestValidToAssignUserRolesToUserGroup(assignUserRoleToUserGroupRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidToRemoveUserRolesFromUserGroup(RemoveUserRolesFromUserGroupRequest removeUserRolesFromUserGroupRequest) {
        return isRequestValidToRemoveUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, Locale.getDefault()).getSuccess();
    }

    default boolean isRequestValidToAddUserToUserGroup(AddUserToUserGroupRequest addUserToUserGroupRequest) {
        return isRequestValidToAddUserToUserGroup(addUserToUserGroupRequest, Locale.getDefault()).getSuccess();
    }
}
