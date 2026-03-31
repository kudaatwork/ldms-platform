package projectlx.user.management.service.business.validation.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.validator.api.UserGroupServiceValidator;
import projectlx.user.management.service.business.validator.impl.UserGroupServiceValidatorImpl;
import projectlx.user.management.service.utils.requests.AddUserToUserGroupRequest;
import projectlx.user.management.service.utils.requests.AssignUserRoleToUserGroupRequest;
import projectlx.user.management.service.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.service.utils.requests.EditUserGroupRequest;
import projectlx.user.management.service.utils.requests.RemoveUserRolesFromUserGroupRequest;
import projectlx.user.management.service.utils.requests.UserGroupMultipleFiltersRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UserGroupServiceValidatorImplTest {

    private UserGroupServiceValidator userGroupServiceValidator;
    private CreateUserGroupRequest createUserGroupRequest;
    private EditUserGroupRequest editUserGroupRequest;
    private UserGroupMultipleFiltersRequest userGroupMultipleFiltersRequest;
    private AssignUserRoleToUserGroupRequest assignUserRoleToUserGroupRequest;
    private RemoveUserRolesFromUserGroupRequest removeUserRolesFromUserGroupRequest;
    private AddUserToUserGroupRequest addUserToUserGroupRequest;
    private Locale locale;

    @Mock
    private MessageService messageService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        userGroupServiceValidator = new UserGroupServiceValidatorImpl(messageService);
        locale = Locale.getDefault();

        // Setup CreateUserGroupRequest with valid data
        createUserGroupRequest = new CreateUserGroupRequest();
        createUserGroupRequest.setName("TestGroup");
        createUserGroupRequest.setDescription("Test group description");

        // Setup EditUserGroupRequest with valid data
        editUserGroupRequest = new EditUserGroupRequest();
        editUserGroupRequest.setId(1L);
        editUserGroupRequest.setName("Test1Group");  // Contains a digit as required by validation
        editUserGroupRequest.setDescription("Updated test group description");

        // Setup UserGroupMultipleFiltersRequest with valid data
        userGroupMultipleFiltersRequest = new UserGroupMultipleFiltersRequest();
        userGroupMultipleFiltersRequest.setPage(0);
        userGroupMultipleFiltersRequest.setSize(10);
        userGroupMultipleFiltersRequest.setSearchValue("test");
        userGroupMultipleFiltersRequest.setName("TestGroup");
        userGroupMultipleFiltersRequest.setDescription("Test description");

        // Setup AssignUserRoleToUserGroupRequest with valid data
        assignUserRoleToUserGroupRequest = new AssignUserRoleToUserGroupRequest();
        assignUserRoleToUserGroupRequest.setUserGroupId(1L);
        List<Long> userRoleIds = new ArrayList<>();
        userRoleIds.add(1L);
        userRoleIds.add(2L);
        assignUserRoleToUserGroupRequest.setUserRoleIds(userRoleIds);

        // Setup RemoveUserRolesFromUserGroupRequest with valid data
        removeUserRolesFromUserGroupRequest = new RemoveUserRolesFromUserGroupRequest();
        removeUserRolesFromUserGroupRequest.setUserGroupId(1L);
        removeUserRolesFromUserGroupRequest.setUserRoleIds(userRoleIds);

        // Setup AddUserToUserGroupRequest with valid data
        addUserToUserGroupRequest = new AddUserToUserGroupRequest();
        addUserToUserGroupRequest.setUserGroupId(1L);
        addUserToUserGroupRequest.setUserId(1L);
    }

    // Tests for isCreateUserGroupRequestValid method

    @Test
    public void isCreateUserGroupRequestValid_shouldReturnFalseForNullRequest() {
        createUserGroupRequest = null;

        ValidatorDto result = userGroupServiceValidator.isCreateUserGroupRequestValid(createUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
    }

    @Test
    public void isCreateUserGroupRequestValid_shouldReturnFalseForNullName() {
        createUserGroupRequest.setName(null);

        ValidatorDto result = userGroupServiceValidator.isCreateUserGroupRequestValid(createUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null name");
    }

    @Test
    public void isCreateUserGroupRequestValid_shouldReturnFalseForEmptyName() {
        createUserGroupRequest.setName("");

        ValidatorDto result = userGroupServiceValidator.isCreateUserGroupRequestValid(createUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty name");
    }

    @Test
    public void isCreateUserGroupRequestValid_shouldReturnFalseForNullDescription() {
        createUserGroupRequest.setDescription(null);

        ValidatorDto result = userGroupServiceValidator.isCreateUserGroupRequestValid(createUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null description");
    }

    @Test
    public void isCreateUserGroupRequestValid_shouldReturnFalseForEmptyDescription() {
        createUserGroupRequest.setDescription("");

        ValidatorDto result = userGroupServiceValidator.isCreateUserGroupRequestValid(createUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty description");
    }

    @Test
    public void isCreateUserGroupRequestValid_shouldReturnFalseForNameWithDigits() {
        createUserGroupRequest.setName("TestGroup123");

        ValidatorDto result = userGroupServiceValidator.isCreateUserGroupRequestValid(createUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for name containing digits");
    }

    @Test
    public void isCreateUserGroupRequestValid_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userGroupServiceValidator.isCreateUserGroupRequestValid(createUserGroupRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
    }

    // Tests for isIdValid method

    @Test
    public void isIdValid_shouldReturnFalseForNullId() {
        ValidatorDto result = userGroupServiceValidator.isIdValid(null, locale);

        assertFalse(result.getSuccess(), "Should return false for null ID");
    }

    @Test
    public void isIdValid_shouldReturnFalseForZeroId() {
        ValidatorDto result = userGroupServiceValidator.isIdValid(0L, locale);

        assertFalse(result.getSuccess(), "Should return false for zero ID");
    }

    @Test
    public void isIdValid_shouldReturnFalseForNegativeId() {
        ValidatorDto result = userGroupServiceValidator.isIdValid(-1L, locale);

        assertFalse(result.getSuccess(), "Should return false for negative ID");
    }

    @Test
    public void isIdValid_shouldReturnTrueForPositiveId() {
        ValidatorDto result = userGroupServiceValidator.isIdValid(1L, locale);

        assertTrue(result.getSuccess(), "Should return true for positive ID");
    }

    // Tests for isRequestValidForEditing method

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullRequest() {
        editUserGroupRequest = null;

        ValidatorDto result = userGroupServiceValidator.isRequestValidForEditing(editUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullId() {
        editUserGroupRequest.setId(null);

        ValidatorDto result = userGroupServiceValidator.isRequestValidForEditing(editUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null ID");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForZeroId() {
        editUserGroupRequest.setId(0L);

        ValidatorDto result = userGroupServiceValidator.isRequestValidForEditing(editUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for zero ID");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullName() {
        editUserGroupRequest.setName(null);

        ValidatorDto result = userGroupServiceValidator.isRequestValidForEditing(editUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null name");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyName() {
        editUserGroupRequest.setName("");

        ValidatorDto result = userGroupServiceValidator.isRequestValidForEditing(editUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty name");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullDescription() {
        editUserGroupRequest.setDescription(null);

        ValidatorDto result = userGroupServiceValidator.isRequestValidForEditing(editUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null description");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyDescription() {
        editUserGroupRequest.setDescription("");

        ValidatorDto result = userGroupServiceValidator.isRequestValidForEditing(editUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty description");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNameWithoutDigits() {
        editUserGroupRequest.setName("TestGroup");

        ValidatorDto result = userGroupServiceValidator.isRequestValidForEditing(editUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for name without digits");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userGroupServiceValidator.isRequestValidForEditing(editUserGroupRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
    }

    // Tests for isRequestValidToRetrieveUsersByMultipleFilters method

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnFalseForNullRequest() {
        userGroupMultipleFiltersRequest = null;

        ValidatorDto result = userGroupServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userGroupMultipleFiltersRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnFalseForNegativePage() {
        userGroupMultipleFiltersRequest.setPage(-1);

        ValidatorDto result = userGroupServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userGroupMultipleFiltersRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for negative page number");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userGroupServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userGroupMultipleFiltersRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
    }

    // Tests for isStringValid method

    @Test
    public void isStringValid_shouldReturnFalseForNullString() {
        ValidatorDto result = userGroupServiceValidator.isStringValid(null, locale);

        assertFalse(result.getSuccess(), "Should return false for null string");
    }

    @Test
    public void isStringValid_shouldReturnFalseForEmptyString() {
        ValidatorDto result = userGroupServiceValidator.isStringValid("", locale);

        assertFalse(result.getSuccess(), "Should return false for empty string");
    }

    @Test
    public void isStringValid_shouldReturnTrueForValidString() {
        ValidatorDto result = userGroupServiceValidator.isStringValid("Valid string", locale);

        assertTrue(result.getSuccess(), "Should return true for valid string");
    }

    // Tests for isRequestValidToAssignUserRolesToUserGroup method

    @Test
    public void isRequestValidToAssignUserRolesToUserGroup_shouldReturnFalseForNullRequest() {
        assignUserRoleToUserGroupRequest = null;

        ValidatorDto result = userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(assignUserRoleToUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
    }

    @Test
    public void isRequestValidToAssignUserRolesToUserGroup_shouldReturnFalseForNullUserGroupId() {
        assignUserRoleToUserGroupRequest.setUserGroupId(null);

        ValidatorDto result = userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(assignUserRoleToUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null user group ID");
    }

    @Test
    public void isRequestValidToAssignUserRolesToUserGroup_shouldReturnFalseForZeroUserGroupId() {
        assignUserRoleToUserGroupRequest.setUserGroupId(0L);

        ValidatorDto result = userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(assignUserRoleToUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for zero user group ID");
    }

    @Test
    public void isRequestValidToAssignUserRolesToUserGroup_shouldReturnFalseForNullUserRoleIds() {
        assignUserRoleToUserGroupRequest.setUserRoleIds(null);

        ValidatorDto result = userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(assignUserRoleToUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null user role IDs");
    }

    @Test
    public void isRequestValidToAssignUserRolesToUserGroup_shouldReturnFalseForEmptyUserRoleIds() {
        assignUserRoleToUserGroupRequest.setUserRoleIds(new ArrayList<>());

        ValidatorDto result = userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(assignUserRoleToUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty user role IDs");
    }

    @Test
    public void isRequestValidToAssignUserRolesToUserGroup_shouldReturnFalseForInvalidUserRoleId() {
        List<Long> invalidUserRoleIds = new ArrayList<>();
        invalidUserRoleIds.add(1L);
        invalidUserRoleIds.add(0L);  // Invalid ID
        assignUserRoleToUserGroupRequest.setUserRoleIds(invalidUserRoleIds);

        ValidatorDto result = userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(assignUserRoleToUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for invalid user role ID");
    }

    @Test
    public void isRequestValidToAssignUserRolesToUserGroup_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(assignUserRoleToUserGroupRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
    }

    // Tests for isRequestValidToRemoveUserRolesFromUserGroup method

    @Test
    public void isRequestValidToRemoveUserRolesFromUserGroup_shouldReturnFalseForNullRequest() {
        removeUserRolesFromUserGroupRequest = null;

        ValidatorDto result = userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
    }

    @Test
    public void isRequestValidToRemoveUserRolesFromUserGroup_shouldReturnFalseForNullUserGroupId() {
        removeUserRolesFromUserGroupRequest.setUserGroupId(null);

        ValidatorDto result = userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null user group ID");
    }

    @Test
    public void isRequestValidToRemoveUserRolesFromUserGroup_shouldReturnFalseForZeroUserGroupId() {
        removeUserRolesFromUserGroupRequest.setUserGroupId(0L);

        ValidatorDto result = userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for zero user group ID");
    }

    @Test
    public void isRequestValidToRemoveUserRolesFromUserGroup_shouldReturnFalseForNullUserRoleIds() {
        removeUserRolesFromUserGroupRequest.setUserRoleIds(null);

        boolean result = userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest);

        assertFalse(result, "Should return false for null user role IDs");
    }

    @Test
    public void isRequestValidToRemoveUserRolesFromUserGroup_shouldReturnFalseForEmptyUserRoleIds() {
        removeUserRolesFromUserGroupRequest.setUserRoleIds(new ArrayList<>());

        boolean result = userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest);

        assertFalse(result, "Should return false for empty user role IDs");
    }

    @Test
    public void isRequestValidToRemoveUserRolesFromUserGroup_shouldReturnFalseForInvalidUserRoleId() {
        List<Long> invalidUserRoleIds = new ArrayList<>();
        invalidUserRoleIds.add(1L);
        invalidUserRoleIds.add(0L);  // Invalid ID
        removeUserRolesFromUserGroupRequest.setUserRoleIds(invalidUserRoleIds);

        boolean result = userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest);

        assertFalse(result, "Should return false for invalid user role ID");
    }

    @Test
    public void isRequestValidToRemoveUserRolesFromUserGroup_shouldReturnTrueForValidRequest() {
        boolean result = userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest);

        assertTrue(result, "Should return true for valid request");
    }

    // Tests for isRequestValidToAddUserToUserGroup method

    @Test
    public void isRequestValidToAddUserToUserGroup_shouldReturnFalseForNullRequest() {
        addUserToUserGroupRequest = null;

        boolean result = userGroupServiceValidator.isRequestValidToAddUserToUserGroup(addUserToUserGroupRequest);

        assertFalse(result, "Should return false for null request");
    }

    @Test
    public void isRequestValidToAddUserToUserGroup_shouldReturnFalseForNullUserGroupId() {
        addUserToUserGroupRequest.setUserGroupId(null);

        boolean result = userGroupServiceValidator.isRequestValidToAddUserToUserGroup(addUserToUserGroupRequest);

        assertFalse(result, "Should return false for null user group ID");
    }

    @Test
    public void isRequestValidToAddUserToUserGroup_shouldReturnFalseForZeroUserGroupId() {
        addUserToUserGroupRequest.setUserGroupId(0L);

        boolean result = userGroupServiceValidator.isRequestValidToAddUserToUserGroup(addUserToUserGroupRequest);

        assertFalse(result, "Should return false for zero user group ID");
    }

    @Test
    public void isRequestValidToAddUserToUserGroup_shouldReturnFalseForNullUserId() {
        addUserToUserGroupRequest.setUserId(null);

        boolean result = userGroupServiceValidator.isRequestValidToAddUserToUserGroup(addUserToUserGroupRequest);

        assertFalse(result, "Should return false for null user ID");
    }

    @Test
    public void isRequestValidToAddUserToUserGroup_shouldReturnFalseForZeroUserId() {
        addUserToUserGroupRequest.setUserId(0L);

        boolean result = userGroupServiceValidator.isRequestValidToAddUserToUserGroup(addUserToUserGroupRequest);

        assertFalse(result, "Should return false for zero user ID");
    }

    @Test
    public void isRequestValidToAddUserToUserGroup_shouldReturnTrueForValidRequest() {
        boolean result = userGroupServiceValidator.isRequestValidToAddUserToUserGroup(addUserToUserGroupRequest);

        assertTrue(result, "Should return true for valid request");
    }
}
