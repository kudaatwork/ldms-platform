package projectlx.user.management.business.validation.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.validator.api.UserSecurityServiceValidator;
import projectlx.user.management.business.validator.impl.UserSecurityServiceValidatorImpl;
import projectlx.user.management.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.utils.requests.EditUserSecurityRequest;
import projectlx.user.management.utils.requests.UserSecurityMultipleFiltersRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class UserSecurityServiceValidatorImplTest {

    private UserSecurityServiceValidator userSecurityServiceValidator;
    private CreateUserSecurityRequest createUserSecurityRequest;
    private EditUserSecurityRequest editUserSecurityRequest;
    private UserSecurityMultipleFiltersRequest userSecurityMultipleFiltersRequest;

    @Mock
    private MessageService messageService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(messageService.getMessage(anyString(), any(String[].class), any())).thenReturn("Test message");
        userSecurityServiceValidator = new UserSecurityServiceValidatorImpl(messageService);

        // Setup CreateUserSecurityRequest with valid data
        createUserSecurityRequest = new CreateUserSecurityRequest();
        createUserSecurityRequest.setSecurityQuestion_1("What is your mother's maiden name?");
        createUserSecurityRequest.setSecurityAnswer_1("Smith");
        createUserSecurityRequest.setSecurityQuestion_2("What was your first pet's name?");
        createUserSecurityRequest.setSecurityAnswer_2("Rover");
        createUserSecurityRequest.setTwoFactorAuthSecret("ABCDEFGHIJKLMNOP");
        createUserSecurityRequest.setIsTwoFactorEnabled(true);
        createUserSecurityRequest.setUserId(1L);

        // Setup EditUserSecurityRequest with valid data
        editUserSecurityRequest = new EditUserSecurityRequest();
        editUserSecurityRequest.setId(1L);
        editUserSecurityRequest.setSecurityQuestion_1("What is your mother's maiden name?");
        editUserSecurityRequest.setSecurityAnswer_1("Smith");
        editUserSecurityRequest.setSecurityQuestion_2("What was your first pet's name?");
        editUserSecurityRequest.setSecurityAnswer_2("Rover");
        editUserSecurityRequest.setTwoFactorAuthSecret("ABCDEFGHIJKLMNOP");
        editUserSecurityRequest.setIsTwoFactorEnabled(true);
        editUserSecurityRequest.setUserId(1L);

        // Setup UserSecurityMultipleFiltersRequest with valid data
        userSecurityMultipleFiltersRequest = new UserSecurityMultipleFiltersRequest();
        userSecurityMultipleFiltersRequest.setPage(0);
        userSecurityMultipleFiltersRequest.setSize(10);
        userSecurityMultipleFiltersRequest.setSearchValue("test");
        userSecurityMultipleFiltersRequest.setUserId(1L);
    }

    // Tests for isCreateUserSecurityRequestValid method

    @Test
    public void isCreateUserSecurityRequestValid_shouldReturnFalseForNullRequest() {
        createUserSecurityRequest = null;

        boolean result = userSecurityServiceValidator.isCreateUserSecurityRequestValid(createUserSecurityRequest);

        assertFalse(result, "Should return false for null request");
    }

    @Test
    public void isCreateUserSecurityRequestValid_shouldReturnFalseForNullSecurityQuestion1() {
        createUserSecurityRequest.setSecurityQuestion_1(null);

        boolean result = userSecurityServiceValidator.isCreateUserSecurityRequestValid(createUserSecurityRequest);

        assertFalse(result, "Should return false for null security question 1");
    }

    @Test
    public void isCreateUserSecurityRequestValid_shouldReturnFalseForEmptySecurityQuestion1() {
        createUserSecurityRequest.setSecurityQuestion_1("");

        boolean result = userSecurityServiceValidator.isCreateUserSecurityRequestValid(createUserSecurityRequest);

        assertFalse(result, "Should return false for empty security question 1");
    }

    @Test
    public void isCreateUserSecurityRequestValid_shouldReturnFalseForNullSecurityAnswer1() {
        createUserSecurityRequest.setSecurityAnswer_1(null);

        boolean result = userSecurityServiceValidator.isCreateUserSecurityRequestValid(createUserSecurityRequest);

        assertFalse(result, "Should return false for null security answer 1");
    }

    @Test
    public void isCreateUserSecurityRequestValid_shouldReturnFalseForEmptySecurityAnswer1() {
        createUserSecurityRequest.setSecurityAnswer_1("");

        boolean result = userSecurityServiceValidator.isCreateUserSecurityRequestValid(createUserSecurityRequest);

        assertFalse(result, "Should return false for empty security answer 1");
    }

    @Test
    public void isCreateUserSecurityRequestValid_shouldReturnFalseForNullTwoFactorAuthSecret() {
        createUserSecurityRequest.setTwoFactorAuthSecret(null);

        boolean result = userSecurityServiceValidator.isCreateUserSecurityRequestValid(createUserSecurityRequest);

        assertFalse(result, "Should return false for null two factor auth secret");
    }

    @Test
    public void isCreateUserSecurityRequestValid_shouldReturnFalseForEmptyTwoFactorAuthSecret() {
        createUserSecurityRequest.setTwoFactorAuthSecret("");

        boolean result = userSecurityServiceValidator.isCreateUserSecurityRequestValid(createUserSecurityRequest);

        assertFalse(result, "Should return false for empty two factor auth secret");
    }

    @Test
    public void isCreateUserSecurityRequestValid_shouldReturnFalseForNullUserId() {
        createUserSecurityRequest.setUserId(null);

        boolean result = userSecurityServiceValidator.isCreateUserSecurityRequestValid(createUserSecurityRequest);

        assertFalse(result, "Should return false for null user ID");
    }

    @Test
    public void isCreateUserSecurityRequestValid_shouldReturnFalseForZeroUserId() {
        createUserSecurityRequest.setUserId(0L);

        boolean result = userSecurityServiceValidator.isCreateUserSecurityRequestValid(createUserSecurityRequest);

        assertFalse(result, "Should return false for zero user ID");
    }

    @Test
    public void isCreateUserSecurityRequestValid_shouldReturnTrueForValidRequest() {
        boolean result = userSecurityServiceValidator.isCreateUserSecurityRequestValid(createUserSecurityRequest);

        assertTrue(result, "Should return true for valid request");
    }

    // Tests for isIdValid method

    @Test
    public void isIdValid_shouldReturnFalseForNullId() {
        boolean result = userSecurityServiceValidator.isIdValid(null);

        assertFalse(result, "Should return false for null ID");
    }

    @Test
    public void isIdValid_shouldReturnFalseForZeroId() {
        boolean result = userSecurityServiceValidator.isIdValid(0L);

        assertFalse(result, "Should return false for zero ID");
    }

    @Test
    public void isIdValid_shouldReturnFalseForOneId() {
        boolean result = userSecurityServiceValidator.isIdValid(1L);

        assertFalse(result, "Should return false for ID equal to 1");
    }

    @Test
    public void isIdValid_shouldReturnTrueForPositiveId() {
        boolean result = userSecurityServiceValidator.isIdValid(2L);

        assertTrue(result, "Should return true for positive ID greater than 1");
    }

    // Tests for isRequestValidForEditing method

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullRequest() {
        editUserSecurityRequest = null;

        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertFalse(result, "Should return false for null request");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullId() {
        editUserSecurityRequest.setId(null);

        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertFalse(result, "Should return false for null ID");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForZeroId() {
        editUserSecurityRequest.setId(0L);

        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertFalse(result, "Should return false for zero ID");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullSecurityQuestion1() {
        editUserSecurityRequest.setSecurityQuestion_1(null);

        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertFalse(result, "Should return false for null security question 1");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptySecurityQuestion1() {
        editUserSecurityRequest.setSecurityQuestion_1("");

        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertFalse(result, "Should return false for empty security question 1");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullSecurityAnswer1() {
        editUserSecurityRequest.setSecurityAnswer_1(null);

        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertFalse(result, "Should return false for null security answer 1");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptySecurityAnswer1() {
        editUserSecurityRequest.setSecurityAnswer_1("");

        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertFalse(result, "Should return false for empty security answer 1");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullTwoFactorAuthSecret() {
        editUserSecurityRequest.setTwoFactorAuthSecret(null);

        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertFalse(result, "Should return false for null two factor auth secret");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyTwoFactorAuthSecret() {
        editUserSecurityRequest.setTwoFactorAuthSecret("");

        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertFalse(result, "Should return false for empty two factor auth secret");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullUserId() {
        editUserSecurityRequest.setUserId(null);

        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertFalse(result, "Should return false for null user ID");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForZeroUserId() {
        editUserSecurityRequest.setUserId(0L);

        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertFalse(result, "Should return false for zero user ID");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnTrueForValidRequest() {
        boolean result = userSecurityServiceValidator.isRequestValidForEditing(editUserSecurityRequest);

        assertTrue(result, "Should return true for valid request");
    }

    // Tests for isRequestValidToRetrieveUserSecurityByMultipleFilters method

    @Test
    public void isRequestValidToRetrieveUserSecurityByMultipleFilters_shouldReturnFalseForNullRequest() {
        userSecurityMultipleFiltersRequest = null;

        boolean result = userSecurityServiceValidator.isRequestValidToRetrieveUserSecurityByMultipleFilters(userSecurityMultipleFiltersRequest);

        assertFalse(result, "Should return false for null request");
    }

    @Test
    public void isRequestValidToRetrieveUserSecurityByMultipleFilters_shouldReturnFalseForNegativePage() {
        userSecurityMultipleFiltersRequest.setPage(-1);

        boolean result = userSecurityServiceValidator.isRequestValidToRetrieveUserSecurityByMultipleFilters(userSecurityMultipleFiltersRequest);

        assertFalse(result, "Should return false for negative page");
    }

    @Test
    public void isRequestValidToRetrieveUserSecurityByMultipleFilters_shouldReturnTrueForValidRequest() {
        boolean result = userSecurityServiceValidator.isRequestValidToRetrieveUserSecurityByMultipleFilters(userSecurityMultipleFiltersRequest);

        assertTrue(result, "Should return true for valid request");
    }

    // Tests for isStringValid method

    @Test
    public void isStringValid_shouldReturnFalseForNullString() {
        boolean result = userSecurityServiceValidator.isStringValid(null);

        assertFalse(result, "Should return false for null string");
    }

    @Test
    public void isStringValid_shouldReturnFalseForEmptyString() {
        boolean result = userSecurityServiceValidator.isStringValid("");

        assertFalse(result, "Should return false for empty string");
    }

    @Test
    public void isStringValid_shouldReturnFalseForBlankString() {
        boolean result = userSecurityServiceValidator.isStringValid("   ");

        assertFalse(result, "Should return false for blank string");
    }

    @Test
    public void isStringValid_shouldReturnTrueForValidString() {
        boolean result = userSecurityServiceValidator.isStringValid("valid string");

        assertTrue(result, "Should return true for valid string");
    }

    // Tests for isBooleanValid method

    @Test
    public void isBooleanValid_shouldReturnFalseForNullBoolean() {
        boolean result = userSecurityServiceValidator.isBooleanValid(null);

        assertFalse(result, "Should return false for null boolean");
    }

    @Test
    public void isBooleanValid_shouldReturnTrueForTrueBoolean() {
        boolean result = userSecurityServiceValidator.isBooleanValid(true);

        assertTrue(result, "Should return true for true boolean");
    }

    @Test
    public void isBooleanValid_shouldReturnTrueForFalseBoolean() {
        boolean result = userSecurityServiceValidator.isBooleanValid(false);

        assertTrue(result, "Should return true for false boolean");
    }
}
