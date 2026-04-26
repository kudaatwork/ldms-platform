package projectlx.user.management.business.validation.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.validator.api.UserRoleServiceValidator;
import projectlx.user.management.business.validator.impl.UserRoleServiceValidatorImpl;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserRoleRequest;
import projectlx.user.management.utils.requests.EditUserRoleRequest;
import projectlx.user.management.utils.requests.UserRoleMultipleFiltersRequest;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserRoleServiceValidatorImplTest {

    private UserRoleServiceValidator userRoleServiceValidator;
    private MessageService messageService;
    private CreateUserRoleRequest createUserRoleRequest;
    private EditUserRoleRequest editUserRoleRequest;
    private UserRoleMultipleFiltersRequest userRoleMultipleFiltersRequest;
    private Locale locale;

    @BeforeEach
    public void setup() {
        messageService = mock(MessageService.class);
        userRoleServiceValidator = new UserRoleServiceValidatorImpl(messageService);
        locale = Locale.ENGLISH;

        // Mock message service responses
        when(messageService.getMessage(anyString(), any(String[].class), any(Locale.class))).thenReturn("Mocked message");
        when(messageService.getMessage(eq(I18Code.MESSAGE_CREATE_USER_ROLE_REQUEST_IS_NULL.getCode()), any(String[].class), any(Locale.class))).thenReturn("CreateUserRoleRequest is null");
        when(messageService.getMessage(eq(I18Code.MESSAGE_CREATE_USER_ROLE_NAME_MISSING.getCode()), any(String[].class), any(Locale.class))).thenReturn("Role name is missing");
        when(messageService.getMessage(eq(I18Code.MESSAGE_CREATE_USER_ROLE_DESCRIPTION_MISSING.getCode()), any(String[].class), any(Locale.class))).thenReturn("Role description is missing");
        when(messageService.getMessage(eq(I18Code.MESSAGE_CREATE_USER_ROLE_NAME_CONTAINS_DIGITS.getCode()), any(String[].class), any(Locale.class))).thenReturn("Role name contains digits");
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()), any(String[].class), any(Locale.class))).thenReturn("ID is invalid");
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_ROLE_REQUEST_IS_NULL.getCode()), any(String[].class), any(Locale.class))).thenReturn("EditUserRoleRequest is null");
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_ROLE_ID_INVALID.getCode()), any(String[].class), any(Locale.class))).thenReturn("Role ID is invalid");
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_ROLE_NAME_MISSING.getCode()), any(String[].class), any(Locale.class))).thenReturn("Role name is missing for editing");
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_ROLE_DESCRIPTION_MISSING.getCode()), any(String[].class), any(Locale.class))).thenReturn("Role description is missing for editing");
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_ROLE_NAME_CONTAINS_DIGITS.getCode()), any(String[].class), any(Locale.class))).thenReturn("Role name contains digits for editing");
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode()), any(String[].class), any(Locale.class))).thenReturn("UserRoleMultipleFiltersRequest is null");
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_MULTIPLE_FILTERS_PAGE_LESS_THAN_ZERO.getCode()), any(String[].class), any(Locale.class))).thenReturn("Page number is less than 0");
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_STRING_INVALID.getCode()), any(String[].class), any(Locale.class))).thenReturn("String is invalid");

        // Setup CreateUserRoleRequest with valid data
        createUserRoleRequest = new CreateUserRoleRequest();
        createUserRoleRequest.setRole("ADMIN");
        createUserRoleRequest.setDescription("Administrator role with full access");

        // Setup EditUserRoleRequest with valid data
        editUserRoleRequest = new EditUserRoleRequest();
        editUserRoleRequest.setId(1L);
        editUserRoleRequest.setRole("ADMIN");
        editUserRoleRequest.setDescription("Administrator role with full access");

        // Setup UserRoleMultipleFiltersRequest with valid data
        userRoleMultipleFiltersRequest = new UserRoleMultipleFiltersRequest();
        userRoleMultipleFiltersRequest.setPage(0);
        userRoleMultipleFiltersRequest.setSize(10);
        userRoleMultipleFiltersRequest.setSearchValue("admin");
        userRoleMultipleFiltersRequest.setRole("ADMIN");
        userRoleMultipleFiltersRequest.setDescription("Administrator");
    }

    // Tests for isCreateUserRoleRequestValid method

    @Test
    public void isCreateUserRoleRequestValid_shouldReturnFalseForNullRequest() {
        createUserRoleRequest = null;

        ValidatorDto result = userRoleServiceValidator.isCreateUserRoleRequestValid(createUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages());
        assertEquals(1, result.getErrorMessages().size());
        assertEquals("CreateUserRoleRequest is null", result.getErrorMessages().get(0));
    }

    @Test
    public void isCreateUserRoleRequestValid_shouldReturnFalseForNullRole() {
        createUserRoleRequest.setRole(null);

        ValidatorDto result = userRoleServiceValidator.isCreateUserRoleRequestValid(createUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for null role");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role name is missing"));
    }

    @Test
    public void isCreateUserRoleRequestValid_shouldReturnFalseForEmptyRole() {
        createUserRoleRequest.setRole("");

        ValidatorDto result = userRoleServiceValidator.isCreateUserRoleRequestValid(createUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for empty role");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role name is missing"));
    }

    @Test
    public void isCreateUserRoleRequestValid_shouldReturnFalseForNullDescription() {
        createUserRoleRequest.setDescription(null);

        ValidatorDto result = userRoleServiceValidator.isCreateUserRoleRequestValid(createUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for null description");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role description is missing"));
    }

    @Test
    public void isCreateUserRoleRequestValid_shouldReturnFalseForEmptyDescription() {
        createUserRoleRequest.setDescription("");

        ValidatorDto result = userRoleServiceValidator.isCreateUserRoleRequestValid(createUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for empty description");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role description is missing"));
    }

    @Test
    public void isCreateUserRoleRequestValid_shouldReturnFalseForRoleWithDigits() {
        createUserRoleRequest.setRole("ADMIN123");

        ValidatorDto result = userRoleServiceValidator.isCreateUserRoleRequestValid(createUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for role containing digits");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role name contains digits"));
    }

    @Test
    public void isCreateUserRoleRequestValid_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userRoleServiceValidator.isCreateUserRoleRequestValid(createUserRoleRequest, locale);

        assertNotNull(result);
        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertTrue(result.getErrorMessages() == null || result.getErrorMessages().isEmpty());
    }

    // Tests for isIdValid method

    @Test
    public void isIdValid_shouldReturnFalseForNullId() {
        ValidatorDto result = userRoleServiceValidator.isIdValid(null, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for null ID");
        assertNotNull(result.getErrorMessages());
        assertEquals(1, result.getErrorMessages().size());
        assertEquals("ID is invalid", result.getErrorMessages().get(0));
    }

    @Test
    public void isIdValid_shouldReturnFalseForIdLessThanOrEqualToOne() {
        ValidatorDto result = userRoleServiceValidator.isIdValid(1L, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for ID less than or equal to 1");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("ID is invalid"));
    }

    @Test
    public void isIdValid_shouldReturnFalseForZeroId() {
        ValidatorDto result = userRoleServiceValidator.isIdValid(0L, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for zero ID");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("ID is invalid"));
    }

    @Test
    public void isIdValid_shouldReturnFalseForNegativeId() {
        ValidatorDto result = userRoleServiceValidator.isIdValid(-1L, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for negative ID");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("ID is invalid"));
    }

    @Test
    public void isIdValid_shouldReturnTrueForIdGreaterThanOne() {
        ValidatorDto result = userRoleServiceValidator.isIdValid(2L, locale);

        assertNotNull(result);
        assertTrue(result.getSuccess(), "Should return true for ID greater than 1");
        assertTrue(result.getErrorMessages() == null || result.getErrorMessages().isEmpty());
    }

    // Tests for isRequestValidForEditing method

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullRequest() {
        editUserRoleRequest = null;

        ValidatorDto result = userRoleServiceValidator.isRequestValidForEditing(editUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages());
        assertEquals(1, result.getErrorMessages().size());
        assertEquals("EditUserRoleRequest is null", result.getErrorMessages().get(0));
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullId() {
        editUserRoleRequest.setId(null);

        ValidatorDto result = userRoleServiceValidator.isRequestValidForEditing(editUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for null ID");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role ID is invalid"));
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForZeroId() {
        editUserRoleRequest.setId(0L);

        ValidatorDto result = userRoleServiceValidator.isRequestValidForEditing(editUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for zero ID");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role ID is invalid"));
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNegativeId() {
        editUserRoleRequest.setId(-1L);

        ValidatorDto result = userRoleServiceValidator.isRequestValidForEditing(editUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for negative ID");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role ID is invalid"));
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullRole() {
        editUserRoleRequest.setRole(null);

        ValidatorDto result = userRoleServiceValidator.isRequestValidForEditing(editUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for null role");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role name is missing for editing"));
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyRole() {
        editUserRoleRequest.setRole("");

        ValidatorDto result = userRoleServiceValidator.isRequestValidForEditing(editUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for empty role");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role name is missing for editing"));
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullDescription() {
        editUserRoleRequest.setDescription(null);

        ValidatorDto result = userRoleServiceValidator.isRequestValidForEditing(editUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for null description");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role description is missing for editing"));
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyDescription() {
        editUserRoleRequest.setDescription("");

        ValidatorDto result = userRoleServiceValidator.isRequestValidForEditing(editUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for empty description");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role description is missing for editing"));
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForRoleWithDigits() {
        editUserRoleRequest.setRole("ADMIN123");

        ValidatorDto result = userRoleServiceValidator.isRequestValidForEditing(editUserRoleRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for role containing digits");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Role name contains digits for editing"));
    }

    @Test
    public void isRequestValidForEditing_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userRoleServiceValidator.isRequestValidForEditing(editUserRoleRequest, locale);

        assertNotNull(result);
        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertTrue(result.getErrorMessages() == null || result.getErrorMessages().isEmpty());
    }

    // Tests for isRequestValidToRetrieveUserRoleByMultipleFilters method

    @Test
    public void isRequestValidToRetrieveUserRoleByMultipleFilters_shouldReturnFalseForNullRequest() {
        userRoleMultipleFiltersRequest = null;

        ValidatorDto result = userRoleServiceValidator.isRequestValidToRetrieveUserRoleByMultipleFilters(userRoleMultipleFiltersRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages());
        assertEquals(1, result.getErrorMessages().size());
        assertEquals("UserRoleMultipleFiltersRequest is null", result.getErrorMessages().get(0));
    }

    @Test
    public void isRequestValidToRetrieveUserRoleByMultipleFilters_shouldReturnFalseForNegativePage() {
        userRoleMultipleFiltersRequest.setPage(-1);

        ValidatorDto result = userRoleServiceValidator.isRequestValidToRetrieveUserRoleByMultipleFilters(userRoleMultipleFiltersRequest, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for negative page number");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("Page number is less than 0"));
    }

    @Test
    public void isRequestValidToRetrieveUserRoleByMultipleFilters_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userRoleServiceValidator.isRequestValidToRetrieveUserRoleByMultipleFilters(userRoleMultipleFiltersRequest, locale);

        assertNotNull(result);
        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertTrue(result.getErrorMessages() == null || result.getErrorMessages().isEmpty());
    }

    // Tests for isStringValid method

    @Test
    public void isStringValid_shouldReturnFalseForNullString() {
        ValidatorDto result = userRoleServiceValidator.isStringValid(null, locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for null string");
        assertNotNull(result.getErrorMessages());
        assertEquals(1, result.getErrorMessages().size());
        assertEquals("String is invalid", result.getErrorMessages().get(0));
    }

    @Test
    public void isStringValid_shouldReturnFalseForEmptyString() {
        ValidatorDto result = userRoleServiceValidator.isStringValid("", locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for empty string");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("String is invalid"));
    }

    @Test
    public void isStringValid_shouldReturnFalseForBlankString() {
        ValidatorDto result = userRoleServiceValidator.isStringValid("   ", locale);

        assertNotNull(result);
        assertFalse(result.getSuccess(), "Should return false for blank string");
        assertNotNull(result.getErrorMessages());
        assertTrue(result.getErrorMessages().contains("String is invalid"));
    }

    @Test
    public void isStringValid_shouldReturnTrueForValidString() {
        ValidatorDto result = userRoleServiceValidator.isStringValid("Valid String", locale);

        assertNotNull(result);
        assertTrue(result.getSuccess(), "Should return true for valid string");
        assertTrue(result.getErrorMessages() == null || result.getErrorMessages().isEmpty());
    }
}
