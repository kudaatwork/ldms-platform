package projectlx.user.management.service.business.validation.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.validator.api.UserPreferencesServiceValidator;
import projectlx.user.management.service.business.validator.impl.UserPreferencesServiceValidatorImpl;
import projectlx.user.management.service.utils.requests.CreateUserPreferencesRequest;
import projectlx.user.management.service.utils.requests.EditUserPreferencesRequest;
import projectlx.user.management.service.utils.requests.UserPreferencesMultipleFiltersRequest;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserPreferencesServiceValidatorImplTest {

    private UserPreferencesServiceValidator userPreferencesServiceValidator;
    private CreateUserPreferencesRequest createUserPreferencesRequest;
    private EditUserPreferencesRequest editUserPreferencesRequest;
    private UserPreferencesMultipleFiltersRequest userPreferencesMultipleFiltersRequest;
    private Locale locale;

    @Mock
    private MessageService messageService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        userPreferencesServiceValidator = new UserPreferencesServiceValidatorImpl(messageService);
        locale = Locale.getDefault();

        // Setup CreateUserPreferencesRequest with valid data
        createUserPreferencesRequest = new CreateUserPreferencesRequest();
        createUserPreferencesRequest.setPreferredLanguage("en");
        createUserPreferencesRequest.setTimezone("UTC");
        createUserPreferencesRequest.setUserId(1L);

        // Setup EditUserPreferencesRequest with valid data
        editUserPreferencesRequest = new EditUserPreferencesRequest();
        editUserPreferencesRequest.setId(1L);
        editUserPreferencesRequest.setPreferredLanguage("en");
        editUserPreferencesRequest.setTimezone("UTC");
        editUserPreferencesRequest.setUserId(1L);

        // Setup UserPreferencesMultipleFiltersRequest with valid data
        userPreferencesMultipleFiltersRequest = new UserPreferencesMultipleFiltersRequest();
        userPreferencesMultipleFiltersRequest.setPage(0);
        userPreferencesMultipleFiltersRequest.setPreferredLanguage("en");
        userPreferencesMultipleFiltersRequest.setTimezone("UTC");
    }

    // Tests for isCreateUserPreferencesRequestValid method

    @Test
    public void isCreateUserPreferencesRequestValid_shouldReturnFalseForNullRequest() {
        createUserPreferencesRequest = null;

        ValidatorDto result = userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(createUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isCreateUserPreferencesRequestValid_shouldReturnFalseForNullPreferredLanguage() {
        createUserPreferencesRequest.setPreferredLanguage(null);

        ValidatorDto result = userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(createUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null preferred language");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isCreateUserPreferencesRequestValid_shouldReturnFalseForEmptyPreferredLanguage() {
        createUserPreferencesRequest.setPreferredLanguage("");

        ValidatorDto result = userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(createUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty preferred language");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isCreateUserPreferencesRequestValid_shouldReturnFalseForNullTimezone() {
        createUserPreferencesRequest.setTimezone(null);

        ValidatorDto result = userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(createUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null timezone");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isCreateUserPreferencesRequestValid_shouldReturnFalseForEmptyTimezone() {
        createUserPreferencesRequest.setTimezone("");

        ValidatorDto result = userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(createUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty timezone");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isCreateUserPreferencesRequestValid_shouldReturnFalseForInvalidTimezone() {
        createUserPreferencesRequest.setTimezone("InvalidTimezone");

        ValidatorDto result = userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(createUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for invalid timezone");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isCreateUserPreferencesRequestValid_shouldReturnFalseForNullUserId() {
        createUserPreferencesRequest.setUserId(null);

        ValidatorDto result = userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(createUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isCreateUserPreferencesRequestValid_shouldReturnFalseForZeroUserId() {
        createUserPreferencesRequest.setUserId(0L);

        ValidatorDto result = userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(createUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for zero user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isCreateUserPreferencesRequestValid_shouldReturnFalseForNegativeUserId() {
        createUserPreferencesRequest.setUserId(-1L);

        ValidatorDto result = userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(createUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for negative user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isCreateUserPreferencesRequestValid_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(createUserPreferencesRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
    }

    // Tests for isIdValid method

    @Test
    public void isIdValid_shouldReturnFalseForNullId() {
        ValidatorDto result = userPreferencesServiceValidator.isIdValid(null, locale);

        assertFalse(result.getSuccess(), "Should return false for null ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isIdValid_shouldReturnFalseForZeroId() {
        ValidatorDto result = userPreferencesServiceValidator.isIdValid(0L, locale);

        assertFalse(result.getSuccess(), "Should return false for zero ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isIdValid_shouldReturnFalseForOneId() {
        ValidatorDto result = userPreferencesServiceValidator.isIdValid(1L, locale);

        assertFalse(result.getSuccess(), "Should return false for ID equal to 1");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isIdValid_shouldReturnTrueForIdGreaterThanOne() {
        ValidatorDto result = userPreferencesServiceValidator.isIdValid(2L, locale);

        assertTrue(result.getSuccess(), "Should return true for ID greater than 1");
    }

    // Tests for isRequestValidForEditing method

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullRequest() {
        editUserPreferencesRequest = null;

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullId() {
        editUserPreferencesRequest.setId(null);

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForZeroId() {
        editUserPreferencesRequest.setId(0L);

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for zero ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullPreferredLanguage() {
        editUserPreferencesRequest.setPreferredLanguage(null);

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null preferred language");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyPreferredLanguage() {
        editUserPreferencesRequest.setPreferredLanguage("");

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty preferred language");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullTimezone() {
        editUserPreferencesRequest.setTimezone(null);

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null timezone");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyTimezone() {
        editUserPreferencesRequest.setTimezone("");

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty timezone");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForInvalidTimezone() {
        editUserPreferencesRequest.setTimezone("InvalidTimezone");

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for invalid timezone");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullUserId() {
        editUserPreferencesRequest.setUserId(null);

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForZeroUserId() {
        editUserPreferencesRequest.setUserId(0L);

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for zero user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNegativeUserId() {
        editUserPreferencesRequest.setUserId(-1L);

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for negative user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userPreferencesServiceValidator.isRequestValidForEditing(editUserPreferencesRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
    }

    // Tests for isRequestValidToRetrieveUserPreferencesByMultipleFilters method

    @Test
    public void isRequestValidToRetrieveUserPreferencesByMultipleFilters_shouldReturnFalseForNullRequest() {
        userPreferencesMultipleFiltersRequest = null;

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidToRetrieveUserPreferencesByMultipleFilters(userPreferencesMultipleFiltersRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidToRetrieveUserPreferencesByMultipleFilters_shouldReturnFalseForNegativePage() {
        userPreferencesMultipleFiltersRequest.setPage(-1);

        ValidatorDto result = userPreferencesServiceValidator.isRequestValidToRetrieveUserPreferencesByMultipleFilters(userPreferencesMultipleFiltersRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for negative page");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isRequestValidToRetrieveUserPreferencesByMultipleFilters_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userPreferencesServiceValidator.isRequestValidToRetrieveUserPreferencesByMultipleFilters(userPreferencesMultipleFiltersRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
    }

    // Tests for isStringValid method

    @Test
    public void isStringValid_shouldReturnFalseForNullString() {
        ValidatorDto result = userPreferencesServiceValidator.isStringValid(null, locale);

        assertFalse(result.getSuccess(), "Should return false for null string");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isStringValid_shouldReturnFalseForEmptyString() {
        ValidatorDto result = userPreferencesServiceValidator.isStringValid("", locale);

        assertFalse(result.getSuccess(), "Should return false for empty string");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isStringValid_shouldReturnFalseForBlankString() {
        ValidatorDto result = userPreferencesServiceValidator.isStringValid("   ", locale);

        assertFalse(result.getSuccess(), "Should return false for blank string");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
    }

    @Test
    public void isStringValid_shouldReturnTrueForValidString() {
        ValidatorDto result = userPreferencesServiceValidator.isStringValid("valid", locale);

        assertTrue(result.getSuccess(), "Should return true for valid string");
    }
}
