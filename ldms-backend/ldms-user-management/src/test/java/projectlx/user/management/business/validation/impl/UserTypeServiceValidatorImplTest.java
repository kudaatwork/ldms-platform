package projectlx.user.management.business.validation.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.validator.api.UserTypeServiceValidator;
import projectlx.user.management.business.validator.impl.UserTypeServiceValidatorImpl;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.utils.requests.EditUserTypeRequest;
import projectlx.user.management.utils.requests.UserTypeMultipleFiltersRequest;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserTypeServiceValidatorImplTest {

    private UserTypeServiceValidator userTypeServiceValidator;
    private CreateUserTypeRequest createUserTypeRequest;
    private EditUserTypeRequest editUserTypeRequest;
    private UserTypeMultipleFiltersRequest userTypeMultipleFiltersRequest;
    private MessageService messageService;
    private Locale locale = Locale.ENGLISH;

    @BeforeEach
    public void setup() {
        messageService = mock(MessageService.class);

        // Setup message service to return error messages
        when(messageService.getMessage(eq(I18Code.MESSAGE_CREATE_USER_TYPE_REQUEST_IS_NULL.getCode()), any(), any())).thenReturn("Create user type request is null");
        when(messageService.getMessage(eq(I18Code.MESSAGE_CREATE_USER_TYPE_NAME_MISSING.getCode()), any(), any())).thenReturn("User type name is missing");
        when(messageService.getMessage(eq(I18Code.MESSAGE_CREATE_USER_TYPE_DESCRIPTION_MISSING.getCode()), any(), any())).thenReturn("User type description is missing");
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_ID_INVALID.getCode()), any(), any())).thenReturn("User type ID is invalid");
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_TYPE_REQUEST_IS_NULL.getCode()), any(), any())).thenReturn("Update user type request is null");
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_TYPE_ID_INVALID.getCode()), any(), any())).thenReturn("Update user type ID is invalid");
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_TYPE_NAME_MISSING.getCode()), any(), any())).thenReturn("Update user type name is missing");
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_TYPE_DESCRIPTION_MISSING.getCode()), any(), any())).thenReturn("Update user type description is missing");
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode()), any(), any())).thenReturn("User type multiple filters request is null");
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_MULTIPLE_FILTERS_PAGE_LESS_THAN_ZERO.getCode()), any(), any())).thenReturn("User type multiple filters page less than zero");
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_STRING_INVALID.getCode()), any(), any())).thenReturn("User type string is invalid");

        userTypeServiceValidator = new UserTypeServiceValidatorImpl(messageService);

        // Setup CreateUserTypeRequest with valid data
        createUserTypeRequest = new CreateUserTypeRequest();
        createUserTypeRequest.setUserTypeName("Admin");
        createUserTypeRequest.setDescription("Administrator user type");

        // Setup EditUserTypeRequest with valid data
        editUserTypeRequest = new EditUserTypeRequest();
        editUserTypeRequest.setId(1L);
        editUserTypeRequest.setUserTypeName("Admin");
        editUserTypeRequest.setDescription("Administrator user type");

        // Setup UserTypeMultipleFiltersRequest with valid data
        userTypeMultipleFiltersRequest = new UserTypeMultipleFiltersRequest();
        userTypeMultipleFiltersRequest.setPage(0);
        userTypeMultipleFiltersRequest.setSize(10);
        userTypeMultipleFiltersRequest.setSearchValue("admin");
        userTypeMultipleFiltersRequest.setUserTypeName("Admin");
        userTypeMultipleFiltersRequest.setDescription("Administrator");
    }

    // Tests for isCreateUserTypeRequestValid method

    @Test
    public void isCreateUserTypeRequestValid_shouldReturnFalseForNullRequest() {
        createUserTypeRequest = null;

        ValidatorDto result = userTypeServiceValidator.isCreateUserTypeRequestValid(createUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("Create user type request is null"), "Should contain appropriate error message");
    }

    @Test
    public void isCreateUserTypeRequestValid_shouldReturnFalseForNullUserTypeName() {
        createUserTypeRequest.setUserTypeName(null);

        ValidatorDto result = userTypeServiceValidator.isCreateUserTypeRequestValid(createUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null user type name");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("User type name is missing"), "Should contain appropriate error message");
    }

    @Test
    public void isCreateUserTypeRequestValid_shouldReturnFalseForEmptyUserTypeName() {
        createUserTypeRequest.setUserTypeName("");

        ValidatorDto result = userTypeServiceValidator.isCreateUserTypeRequestValid(createUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty user type name");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("User type name is missing"), "Should contain appropriate error message");
    }

    @Test
    public void isCreateUserTypeRequestValid_shouldReturnFalseForNullDescription() {
        createUserTypeRequest.setDescription(null);

        ValidatorDto result = userTypeServiceValidator.isCreateUserTypeRequestValid(createUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null description");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("User type description is missing"), "Should contain appropriate error message");
    }

    @Test
    public void isCreateUserTypeRequestValid_shouldReturnFalseForEmptyDescription() {
        createUserTypeRequest.setDescription("");

        ValidatorDto result = userTypeServiceValidator.isCreateUserTypeRequestValid(createUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty description");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("User type description is missing"), "Should contain appropriate error message");
    }

    @Test
    public void isCreateUserTypeRequestValid_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userTypeServiceValidator.isCreateUserTypeRequestValid(createUserTypeRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid request");
    }

    // Tests for isIdValid method

    @Test
    public void isIdValid_shouldReturnFalseForNullId() {
        ValidatorDto result = userTypeServiceValidator.isIdValid(null, locale);

        assertFalse(result.getSuccess(), "Should return false for null ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("User type ID is invalid"), "Should contain appropriate error message");
    }

    @Test
    public void isIdValid_shouldReturnFalseForIdLessThanOrEqualToOne() {
        ValidatorDto result = userTypeServiceValidator.isIdValid(1L, locale);

        assertFalse(result.getSuccess(), "Should return false for ID less than or equal to 1");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("User type ID is invalid"), "Should contain appropriate error message");
    }

    @Test
    public void isIdValid_shouldReturnTrueForIdGreaterThanOne() {
        ValidatorDto result = userTypeServiceValidator.isIdValid(2L, locale);

        assertTrue(result.getSuccess(), "Should return true for ID greater than 1");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid ID");
    }

    // Tests for isRequestValidForEditing method

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullRequest() {
        editUserTypeRequest = null;

        ValidatorDto result = userTypeServiceValidator.isRequestValidForEditing(editUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("Update user type request is null"), "Should contain appropriate error message");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullId() {
        editUserTypeRequest.setId(null);

        ValidatorDto result = userTypeServiceValidator.isRequestValidForEditing(editUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("Update user type ID is invalid"), "Should contain appropriate error message");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForZeroId() {
        editUserTypeRequest.setId(0L);

        ValidatorDto result = userTypeServiceValidator.isRequestValidForEditing(editUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for zero ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("Update user type ID is invalid"), "Should contain appropriate error message");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNegativeId() {
        editUserTypeRequest.setId(-1L);

        ValidatorDto result = userTypeServiceValidator.isRequestValidForEditing(editUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for negative ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("Update user type ID is invalid"), "Should contain appropriate error message");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullUserTypeName() {
        editUserTypeRequest.setUserTypeName(null);

        ValidatorDto result = userTypeServiceValidator.isRequestValidForEditing(editUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null user type name");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("Update user type name is missing"), "Should contain appropriate error message");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyUserTypeName() {
        editUserTypeRequest.setUserTypeName("");

        ValidatorDto result = userTypeServiceValidator.isRequestValidForEditing(editUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty user type name");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("Update user type name is missing"), "Should contain appropriate error message");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullDescription() {
        editUserTypeRequest.setDescription(null);

        ValidatorDto result = userTypeServiceValidator.isRequestValidForEditing(editUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null description");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("Update user type description is missing"), "Should contain appropriate error message");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyDescription() {
        editUserTypeRequest.setDescription("");

        ValidatorDto result = userTypeServiceValidator.isRequestValidForEditing(editUserTypeRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty description");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("Update user type description is missing"), "Should contain appropriate error message");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userTypeServiceValidator.isRequestValidForEditing(editUserTypeRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid request");
    }

    // Tests for isRequestValidToRetrieveUsersByMultipleFilters method

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnFalseForNullRequest() {
        userTypeMultipleFiltersRequest = null;

        ValidatorDto result = userTypeServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userTypeMultipleFiltersRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("User type multiple filters request is null"), "Should contain appropriate error message");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnFalseForNegativePage() {
        userTypeMultipleFiltersRequest.setPage(-1);

        ValidatorDto result = userTypeServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userTypeMultipleFiltersRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for negative page");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("User type multiple filters page less than zero"), "Should contain appropriate error message");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnTrueForZeroPage() {
        userTypeMultipleFiltersRequest.setPage(0);

        ValidatorDto result = userTypeServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userTypeMultipleFiltersRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for zero page");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid request");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnTrueForPositivePage() {
        userTypeMultipleFiltersRequest.setPage(1);

        ValidatorDto result = userTypeServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userTypeMultipleFiltersRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for positive page");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid request");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userTypeServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userTypeMultipleFiltersRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid request");
    }

    // Tests for isStringValid method

    @Test
    public void isStringValid_shouldReturnFalseForNullString() {
        ValidatorDto result = userTypeServiceValidator.isStringValid(null, locale);

        assertFalse(result.getSuccess(), "Should return false for null string");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("User type string is invalid"), "Should contain appropriate error message");
    }

    @Test
    public void isStringValid_shouldReturnFalseForEmptyString() {
        ValidatorDto result = userTypeServiceValidator.isStringValid("", locale);

        assertFalse(result.getSuccess(), "Should return false for empty string");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("User type string is invalid"), "Should contain appropriate error message");
    }

    @Test
    public void isStringValid_shouldReturnFalseForBlankString() {
        ValidatorDto result = userTypeServiceValidator.isStringValid("   ", locale);

        assertFalse(result.getSuccess(), "Should return false for blank string");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
        assertTrue(result.getErrorMessages().contains("User type string is invalid"), "Should contain appropriate error message");
    }

    @Test
    public void isStringValid_shouldReturnTrueForNonEmptyString() {
        ValidatorDto result = userTypeServiceValidator.isStringValid("test", locale);

        assertTrue(result.getSuccess(), "Should return true for non-empty string");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid string");
    }

    @Test
    public void isStringValid_shouldReturnTrueForStringWithWhitespace() {
        ValidatorDto result = userTypeServiceValidator.isStringValid("test string", locale);

        assertTrue(result.getSuccess(), "Should return true for string with whitespace");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid string");
    }
}
