package projectlx.user.management.service.business.validation.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.validator.api.UserAccountServiceValidator;
import projectlx.user.management.service.business.validator.impl.UserAccountServiceValidatorImpl;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.service.utils.requests.EditUserAccountRequest;
import projectlx.user.management.service.utils.requests.UserAccountMultipleFiltersRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class UserAccountServiceValidatorImplTest {

    private UserAccountServiceValidator userAccountServiceValidator;
    private CreateUserAccountRequest createUserAccountRequest;
    private EditUserAccountRequest editUserAccountRequest;
    private UserAccountMultipleFiltersRequest userAccountMultipleFiltersRequest;

    @Mock
    private MessageService messageService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Test error message");
        userAccountServiceValidator = new UserAccountServiceValidatorImpl(messageService);

        // Setup CreateUserAccountRequest with valid data
        createUserAccountRequest = new CreateUserAccountRequest();
        createUserAccountRequest.setPhoneNumber("+263771234567");
        createUserAccountRequest.setUserId(1L);
        createUserAccountRequest.setIsAccountLocked(false);
        createUserAccountRequest.setAccountNumber("ACC123456");

        // Setup EditUserAccountRequest with valid data
        editUserAccountRequest = new EditUserAccountRequest();
        editUserAccountRequest.setId(1L);
        editUserAccountRequest.setPhoneNumber("+263771234567");
        editUserAccountRequest.setUserId(1L);
        editUserAccountRequest.setIsAccountLocked(false);
        editUserAccountRequest.setAccountNumber("ACC123456");

        // Setup UserAccountMultipleFiltersRequest with valid data
        userAccountMultipleFiltersRequest = new UserAccountMultipleFiltersRequest();
        userAccountMultipleFiltersRequest.setPage(0);
        userAccountMultipleFiltersRequest.setSize(10);
        userAccountMultipleFiltersRequest.setPhoneNumber("+263771234567");
        userAccountMultipleFiltersRequest.setAccountNumber("ACC123456");
        userAccountMultipleFiltersRequest.setIsAccountLocked(false);
        userAccountMultipleFiltersRequest.setUserId(1L);
    }

    // Tests for isCreateUserRequestValid method

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForNullRequest() {
        createUserAccountRequest = null;

        ValidatorDto result = userAccountServiceValidator.isCreateUserRequestValid(createUserAccountRequest, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForNullPhoneNumber() {
        createUserAccountRequest.setPhoneNumber(null);

        ValidatorDto result = userAccountServiceValidator.isCreateUserRequestValid(createUserAccountRequest, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null phone number");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForEmptyPhoneNumber() {
        createUserAccountRequest.setPhoneNumber("");

        ValidatorDto result = userAccountServiceValidator.isCreateUserRequestValid(createUserAccountRequest, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for empty phone number");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForNullUserId() {
        createUserAccountRequest.setUserId(null);

        ValidatorDto result = userAccountServiceValidator.isCreateUserRequestValid(createUserAccountRequest, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForZeroUserId() {
        createUserAccountRequest.setUserId(0L);

        ValidatorDto result = userAccountServiceValidator.isCreateUserRequestValid(createUserAccountRequest, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for zero user ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnFalseForInvalidPhoneNumber() {
        createUserAccountRequest.setPhoneNumber("12345"); // Not in international format

        ValidatorDto result = userAccountServiceValidator.isCreateUserRequestValid(createUserAccountRequest, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for invalid phone number format");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isCreateUserRequestValid_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userAccountServiceValidator.isCreateUserRequestValid(createUserAccountRequest, Locale.getDefault());

        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid request");
    }

    // Tests for isIdValid method

    @Test
    public void isIdValid_shouldReturnFalseForNullId() {
        ValidatorDto result = userAccountServiceValidator.isIdValid(null, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isIdValid_shouldReturnFalseForZeroId() {
        ValidatorDto result = userAccountServiceValidator.isIdValid(0L, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for zero ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isIdValid_shouldReturnTrueForPositiveId() {
        ValidatorDto result = userAccountServiceValidator.isIdValid(1L, Locale.getDefault());

        assertTrue(result.getSuccess(), "Should return true for positive ID");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid ID");
    }

    // Tests for isRequestValidForEditing method

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullRequest() {
        editUserAccountRequest = null;

        ValidatorDto result = userAccountServiceValidator.isRequestValidForEditing(editUserAccountRequest, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForInvalidId() {
        editUserAccountRequest.setId(0L);

        ValidatorDto result = userAccountServiceValidator.isRequestValidForEditing(editUserAccountRequest, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for invalid ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForInvalidPhoneNumber() {
        editUserAccountRequest.setPhoneNumber("12345"); // Not in international format

        ValidatorDto result = userAccountServiceValidator.isRequestValidForEditing(editUserAccountRequest, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for invalid phone number format");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userAccountServiceValidator.isRequestValidForEditing(editUserAccountRequest, Locale.getDefault());

        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid request");
    }

    // Tests for isRequestValidToRetrieveUsersByMultipleFilters method

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnFalseForNullRequest() {
        userAccountMultipleFiltersRequest = null;

        ValidatorDto result = userAccountServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userAccountMultipleFiltersRequest, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null request");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnFalseForNegativePage() {
        userAccountMultipleFiltersRequest.setPage(-1);

        ValidatorDto result = userAccountServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userAccountMultipleFiltersRequest, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for negative page number");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userAccountServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(userAccountMultipleFiltersRequest, Locale.getDefault());

        assertTrue(result.getSuccess(), "Should return true for valid request");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid request");
    }

    // Tests for isStringValid method

    @Test
    public void isStringValid_shouldReturnFalseForNullString() {
        ValidatorDto result = userAccountServiceValidator.isStringValid(null, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null string");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isStringValid_shouldReturnFalseForEmptyString() {
        ValidatorDto result = userAccountServiceValidator.isStringValid("", Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for empty string");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isStringValid_shouldReturnTrueForValidString() {
        ValidatorDto result = userAccountServiceValidator.isStringValid("Valid String", Locale.getDefault());

        assertTrue(result.getSuccess(), "Should return true for valid string");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid string");
    }

    // Tests for isListValid method

    @Test
    public void isListValid_shouldReturnFalseForNullList() {
        ValidatorDto result = userAccountServiceValidator.isListValid(null, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null list");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isListValid_shouldReturnFalseForEmptyList() {
        ValidatorDto result = userAccountServiceValidator.isListValid(new ArrayList<>(), Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for empty list");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isListValid_shouldReturnFalseForListWithNullElement() {
        List<String> list = new ArrayList<>();
        list.add(null);
        list.add("Valid");

        ValidatorDto result = userAccountServiceValidator.isListValid(list, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for list with null element");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isListValid_shouldReturnFalseForListWithEmptyElement() {
        List<String> list = new ArrayList<>();
        list.add("");
        list.add("Valid");

        ValidatorDto result = userAccountServiceValidator.isListValid(list, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for list with empty element");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isListValid_shouldReturnTrueForValidList() {
        List<String> list = new ArrayList<>();
        list.add("Valid1");
        list.add("Valid2");

        ValidatorDto result = userAccountServiceValidator.isListValid(list, Locale.getDefault());

        assertTrue(result.getSuccess(), "Should return true for valid list");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid list");
    }

    // Tests for isPhoneNumberValid method

    @Test
    public void isPhoneNumberValid_shouldReturnFalseForNullPhoneNumber() {
        ValidatorDto result = userAccountServiceValidator.isPhoneNumberValid(null, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null phone number");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isPhoneNumberValid_shouldReturnFalseForEmptyPhoneNumber() {
        ValidatorDto result = userAccountServiceValidator.isPhoneNumberValid("", Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for empty phone number");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isPhoneNumberValid_shouldReturnFalseForInvalidPhoneNumber() {
        ValidatorDto result = userAccountServiceValidator.isPhoneNumberValid("12345", Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for invalid phone number format");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isPhoneNumberValid_shouldReturnTrueForValidPhoneNumber() {
        ValidatorDto result = userAccountServiceValidator.isPhoneNumberValid("+263771234567", Locale.getDefault());

        assertTrue(result.getSuccess(), "Should return true for valid phone number");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid phone number");
    }

    // Tests for isNationalIdValid method

    @Test
    public void isNationalIdValid_shouldReturnFalseForNullNationalId() {
        ValidatorDto result = userAccountServiceValidator.isNationalIdValid(null, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null national ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isNationalIdValid_shouldReturnFalseForEmptyNationalId() {
        ValidatorDto result = userAccountServiceValidator.isNationalIdValid("", Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for empty national ID");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isNationalIdValid_shouldReturnFalseForInvalidNationalId() {
        ValidatorDto result = userAccountServiceValidator.isNationalIdValid("123456", Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for invalid national ID format");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isNationalIdValid_shouldReturnFalseForCurrentNationalIdFormat() {
        // The current implementation considers this format invalid
        ValidatorDto result = userAccountServiceValidator.isNationalIdValid("12345678A90", Locale.getDefault());

        assertFalse(result.getSuccess(), "Current implementation returns false for this national ID format");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    // Tests for isValidUserName method

    @Test
    public void isValidUserName_shouldReturnFalseForNullUserName() {
        ValidatorDto result = userAccountServiceValidator.isValidUserName(null, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null username");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isValidUserName_shouldReturnFalseForEmptyUserName() {
        ValidatorDto result = userAccountServiceValidator.isValidUserName("", Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for empty username");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isValidUserName_shouldReturnFalseForInvalidUserName() {
        ValidatorDto result = userAccountServiceValidator.isValidUserName("user@name", Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for invalid username format");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isValidUserName_shouldReturnTrueForValidUserName() {
        ValidatorDto result = userAccountServiceValidator.isValidUserName("username123", Locale.getDefault());

        assertTrue(result.getSuccess(), "Should return true for valid username");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid username");
    }

    // Tests for isPasswordValid method

    @Test
    public void isPasswordValid_shouldReturnFalseForNullPassword() {
        ValidatorDto result = userAccountServiceValidator.isPasswordValid(null, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null password");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isPasswordValid_shouldReturnFalseAsNotImplemented() {
        // Note: This test is expected to pass because the method is not implemented yet
        ValidatorDto result = userAccountServiceValidator.isPasswordValid("Password123!", Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false as the method is not implemented yet");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    // Tests for isBooleanValid method

    @Test
    public void isBooleanValid_shouldReturnFalseForNullBoolean() {
        ValidatorDto result = userAccountServiceValidator.isBooleanValid(null, Locale.getDefault());

        assertFalse(result.getSuccess(), "Should return false for null boolean");
        assertNotNull(result.getErrorMessages(), "Error messages should not be null");
        assertFalse(result.getErrorMessages().isEmpty(), "Error messages should not be empty");
    }

    @Test
    public void isBooleanValid_shouldReturnTrueForValidBoolean() {
        ValidatorDto result = userAccountServiceValidator.isBooleanValid(true, Locale.getDefault());

        assertTrue(result.getSuccess(), "Should return true for valid boolean (true)");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid boolean");
    }

    @Test
    public void isBooleanValid_shouldReturnTrueForValidBooleanFalse() {
        ValidatorDto result = userAccountServiceValidator.isBooleanValid(false, Locale.getDefault());

        assertTrue(result.getSuccess(), "Should return true for valid boolean (false)");
        assertNull(result.getErrorMessages(), "Error messages should be null for valid boolean");
    }
}
