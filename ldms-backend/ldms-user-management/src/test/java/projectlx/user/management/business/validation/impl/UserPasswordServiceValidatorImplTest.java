package projectlx.user.management.business.validation.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.validator.api.UserPasswordServiceValidator;
import projectlx.user.management.business.validator.impl.UserPasswordServiceValidatorImpl;
import projectlx.user.management.utils.requests.ChangeUserPasswordRequest;
import projectlx.user.management.utils.requests.CreateUserPasswordRequest;
import projectlx.user.management.utils.requests.UserRoleMultipleFiltersRequest;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class UserPasswordServiceValidatorImplTest {

    private UserPasswordServiceValidator userPasswordServiceValidator;
    private CreateUserPasswordRequest createUserPasswordRequest;
    private ChangeUserPasswordRequest changeUserPasswordRequest;
    private UserRoleMultipleFiltersRequest userRoleMultipleFiltersRequest;
    private Locale locale;

    @Mock
    private MessageService messageService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        userPasswordServiceValidator = new UserPasswordServiceValidatorImpl(messageService);
        locale = Locale.getDefault();

        // Setup CreateUserPasswordRequest with valid data
        createUserPasswordRequest = new CreateUserPasswordRequest();
        createUserPasswordRequest.setPassword("password123");
        createUserPasswordRequest.setUserId(1L);

        // Setup ChangeUserPasswordRequest with valid data
        changeUserPasswordRequest = new ChangeUserPasswordRequest();
        changeUserPasswordRequest.setId(1L);
        changeUserPasswordRequest.setPassword("newPassword123");
        changeUserPasswordRequest.setUserId(1L);

        // Setup UserRoleMultipleFiltersRequest with valid data
        userRoleMultipleFiltersRequest = new UserRoleMultipleFiltersRequest();
        userRoleMultipleFiltersRequest.setPage(0);
        userRoleMultipleFiltersRequest.setSize(10);
        userRoleMultipleFiltersRequest.setSearchValue("test");
        userRoleMultipleFiltersRequest.setRole("ADMIN");
        userRoleMultipleFiltersRequest.setDescription("Administrator");
    }

    // Tests for isCreateUserRequestValid method

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForNullRequest() {
        createUserPasswordRequest = null;

        ValidatorDto result = userPasswordServiceValidator.isCreateUserRequestValid(createUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForNullPassword() {
        createUserPasswordRequest.setPassword(null);

        ValidatorDto result = userPasswordServiceValidator.isCreateUserRequestValid(createUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null password");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForEmptyPassword() {
        createUserPasswordRequest.setPassword("");

        ValidatorDto result = userPasswordServiceValidator.isCreateUserRequestValid(createUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty password");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForNullUserId() {
        createUserPasswordRequest.setUserId(null);

        ValidatorDto result = userPasswordServiceValidator.isCreateUserRequestValid(createUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForZeroUserId() {
        createUserPasswordRequest.setUserId(0L);

        ValidatorDto result = userPasswordServiceValidator.isCreateUserRequestValid(createUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for zero user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForNegativeUserId() {
        createUserPasswordRequest.setUserId(-1L);

        ValidatorDto result = userPasswordServiceValidator.isCreateUserRequestValid(createUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for negative user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userPasswordServiceValidator.isCreateUserRequestValid(createUserPasswordRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid request");
    }

    // Tests for isStringValid method

    @Test
    public void isStringValid_shouldReturnFalseForAnyInput() {
        ValidatorDto result = userPasswordServiceValidator.isStringValid("test", locale);

        assertTrue(result.getSuccess(), "Should return true for valid input");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid input");
    }

    @Test
    public void isStringValid_shouldReturnFalseForNullInput() {
        ValidatorDto result = userPasswordServiceValidator.isStringValid(null, locale);

        assertFalse(result.getSuccess(), "Should return false for null input");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isStringValid_shouldReturnFalseForEmptyInput() {
        ValidatorDto result = userPasswordServiceValidator.isStringValid("", locale);

        assertFalse(result.getSuccess(), "Should return false for empty input");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    // Tests for isIdValid method

    @Test
    public void isIdValid_shouldReturnFalseForNullId() {
        ValidatorDto result = userPasswordServiceValidator.isIdValid(null, locale);

        assertFalse(result.getSuccess(), "Should return false for null ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isIdValid_shouldReturnFalseForZeroId() {
        ValidatorDto result = userPasswordServiceValidator.isIdValid(0L, locale);

        assertFalse(result.getSuccess(), "Should return false for zero ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isIdValid_shouldReturnFalseForNegativeId() {
        ValidatorDto result = userPasswordServiceValidator.isIdValid(-1L, locale);

        assertFalse(result.getSuccess(), "Should return false for negative ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isIdValid_shouldReturnTrueForPositiveId() {
        ValidatorDto result = userPasswordServiceValidator.isIdValid(1L, locale);

        assertTrue(result.getSuccess(), "Should return true for positive ID");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid ID");
    }

    // Tests for isRequestValidToRetrieveUsersByMultipleFilters method

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnFalseForNullRequest() {
        userRoleMultipleFiltersRequest = null;

        ValidatorDto result = userPasswordServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userRoleMultipleFiltersRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnFalseForNegativePage() {
        userRoleMultipleFiltersRequest.setPage(-1);

        ValidatorDto result = userPasswordServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userRoleMultipleFiltersRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for negative page number");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userPasswordServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userRoleMultipleFiltersRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid request");
    }

    // Tests for isRequestValidForEditing method

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullRequest() {
        changeUserPasswordRequest = null;

        ValidatorDto result = userPasswordServiceValidator.isRequestValidForEditing(changeUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullPassword() {
        changeUserPasswordRequest.setPassword(null);

        ValidatorDto result = userPasswordServiceValidator.isRequestValidForEditing(changeUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null password");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyPassword() {
        changeUserPasswordRequest.setPassword("");

        ValidatorDto result = userPasswordServiceValidator.isRequestValidForEditing(changeUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty password");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullUserId() {
        changeUserPasswordRequest.setUserId(null);

        ValidatorDto result = userPasswordServiceValidator.isRequestValidForEditing(changeUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForZeroUserId() {
        changeUserPasswordRequest.setUserId(0L);

        ValidatorDto result = userPasswordServiceValidator.isRequestValidForEditing(changeUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for zero user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNegativeUserId() {
        changeUserPasswordRequest.setUserId(-1L);

        ValidatorDto result = userPasswordServiceValidator.isRequestValidForEditing(changeUserPasswordRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for negative user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userPasswordServiceValidator.isRequestValidForEditing(changeUserPasswordRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid request");
    }
}
