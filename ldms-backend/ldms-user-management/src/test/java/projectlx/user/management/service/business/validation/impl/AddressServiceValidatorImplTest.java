package projectlx.user.management.service.business.validation.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.validator.api.UserAddressServiceValidator;
import projectlx.user.management.service.business.validator.impl.UserAddressServiceValidatorImpl;
import projectlx.user.management.service.utils.requests.CreateAddressRequest;
import projectlx.user.management.service.utils.requests.EditAddressRequest;
import projectlx.user.management.service.utils.requests.AddressMultipleFiltersRequest;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressServiceValidatorImplTest {

    private UserAddressServiceValidator userAddressServiceValidator;
    private CreateAddressRequest createAddressRequest;
    private EditAddressRequest editAddressRequest;
    private AddressMultipleFiltersRequest addressMultipleFiltersRequest;
    private Locale locale;

    @Mock
    private MessageService messageService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        userAddressServiceValidator = new UserAddressServiceValidatorImpl(messageService);
        locale = Locale.getDefault();

        // Setup CreateAddressRequest with valid data
        createAddressRequest = new CreateAddressRequest();
        createAddressRequest.setLine1("123 Main St");
        createAddressRequest.setLine2("Apt 4B");
        createAddressRequest.setPostalCode("10001");
        createAddressRequest.setSuburbId(200L);
        createAddressRequest.setGeoCoordinatesId(300L);

        // Setup EditAddressRequest with valid data
        editAddressRequest = new EditAddressRequest();
        editAddressRequest.setId(1L);
        editAddressRequest.setLocationAddressId(100L);
        editAddressRequest.setLine1("123 Main St");
        editAddressRequest.setLine2("Apt 4B");
        editAddressRequest.setPostalCode("10001");
        editAddressRequest.setSuburbId(200L);
        editAddressRequest.setGeoCoordinatesId(300L);

        // Setup AddressMultipleFiltersRequest with valid data
        addressMultipleFiltersRequest = new AddressMultipleFiltersRequest();
        addressMultipleFiltersRequest.setPage(0);
        addressMultipleFiltersRequest.setSize(10);
        addressMultipleFiltersRequest.setSearchValue("test");
        addressMultipleFiltersRequest.setLine1("123 Main St");
        addressMultipleFiltersRequest.setLine2("Apt 4B");
        addressMultipleFiltersRequest.setPostalCode("10001");
        addressMultipleFiltersRequest.setEntityStatus(projectlx.co.zw.shared_library.utils.enums.EntityStatus.ACTIVE);
    }

    // Tests for isCreateUserAddressRequestValid method

    @Test
    public void isCreateUserAddressRequestValid_shouldReturnFalseForNullRequest() {
        createAddressRequest = null;

        ValidatorDto result = userAddressServiceValidator.isCreateAddressRequestValid(createAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
    }

    @Test
    public void isCreateUserAddressRequestValid_shouldReturnFalseForNullLine1() {
        createAddressRequest.setLine1(null);

        ValidatorDto result = userAddressServiceValidator.isCreateAddressRequestValid(createAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null line1");
    }

    @Test
    public void isCreateUserAddressRequestValid_shouldReturnFalseForEmptyLine1() {
        createAddressRequest.setLine1("");

        ValidatorDto result = userAddressServiceValidator.isCreateAddressRequestValid(createAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty line1");
    }

    @Test
    public void isCreateUserAddressRequestValid_shouldReturnFalseForNullPostalCode() {
        createAddressRequest.setPostalCode(null);

        ValidatorDto result = userAddressServiceValidator.isCreateAddressRequestValid(createAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null postal code");
    }

    @Test
    public void isCreateUserAddressRequestValid_shouldReturnFalseForEmptyPostalCode() {
        createAddressRequest.setPostalCode("");

        ValidatorDto result = userAddressServiceValidator.isCreateAddressRequestValid(createAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty postal code");
    }

    @Test
    public void isCreateUserAddressRequestValid_shouldReturnFalseForNullSuburbId() {
        createAddressRequest.setSuburbId(null);

        ValidatorDto result = userAddressServiceValidator.isCreateAddressRequestValid(createAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null suburbId");
    }

    @Test
    public void isCreateUserAddressRequestValid_shouldAcceptNegativeSuburbId() {
        createAddressRequest.setSuburbId(-1L);

        ValidatorDto result = userAddressServiceValidator.isCreateAddressRequestValid(createAddressRequest, locale);

        assertTrue(result.getSuccess(), "Current implementation accepts negative suburbId");
    }

    @Test
    public void isCreateUserAddressRequestValid_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userAddressServiceValidator.isCreateAddressRequestValid(createAddressRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
    }

    // Tests for isIdValid method

    @Test
    public void isIdValid_shouldReturnFalseForNullId() {
        ValidatorDto result = userAddressServiceValidator.isIdValid(null, locale);

        assertFalse(result.getSuccess(), "Should return false for null ID");
    }

    @Test
    public void isIdValid_shouldReturnFalseForZeroId() {
        ValidatorDto result = userAddressServiceValidator.isIdValid(0L, locale);

        assertFalse(result.getSuccess(), "Should return false for zero ID");
    }

    @Test
    public void isIdValid_shouldReturnFalseForNegativeId() {
        ValidatorDto result = userAddressServiceValidator.isIdValid(-1L, locale);

        assertFalse(result.getSuccess(), "Should return false for negative ID");
    }

    @Test
    public void isIdValid_shouldReturnTrueForPositiveId() {
        ValidatorDto result = userAddressServiceValidator.isIdValid(1L, locale);

        assertTrue(result.getSuccess(), "Should return true for positive ID");
    }

    // Tests for isRequestValidForEditing method

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullRequest() {
        editAddressRequest = null;

        ValidatorDto result = userAddressServiceValidator.isRequestValidForEditing(editAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullLine1() {
        editAddressRequest.setLine1(null);

        ValidatorDto result = userAddressServiceValidator.isRequestValidForEditing(editAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null line1");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyLine1() {
        editAddressRequest.setLine1("");

        ValidatorDto result = userAddressServiceValidator.isRequestValidForEditing(editAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty line1");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullPostalCode() {
        editAddressRequest.setPostalCode(null);

        ValidatorDto result = userAddressServiceValidator.isRequestValidForEditing(editAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null postal code");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForEmptyPostalCode() {
        editAddressRequest.setPostalCode("");

        ValidatorDto result = userAddressServiceValidator.isRequestValidForEditing(editAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for empty postal code");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullSuburbId() {
        editAddressRequest.setSuburbId(null);

        ValidatorDto result = userAddressServiceValidator.isRequestValidForEditing(editAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null suburbId");
    }

    @Test
    public void isRequestValidForEditing_shouldAcceptNegativeSuburbId() {
        editAddressRequest.setSuburbId(-1L);

        ValidatorDto result = userAddressServiceValidator.isRequestValidForEditing(editAddressRequest, locale);

        assertTrue(result.getSuccess(), "Current implementation accepts negative suburbId");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForNullId() {
        editAddressRequest.setId(null);

        ValidatorDto result = userAddressServiceValidator.isRequestValidForEditing(editAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null id");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnFalseForInvalidId() {
        editAddressRequest.setId(-1L);

        ValidatorDto result = userAddressServiceValidator.isRequestValidForEditing(editAddressRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for invalid id");
    }

    @Test
    public void isRequestValidForEditing_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userAddressServiceValidator.isRequestValidForEditing(editAddressRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
    }

    // Tests for isRequestValidToRetrieveUsersByMultipleFilters method

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnFalseForNullRequest() {
        addressMultipleFiltersRequest = null;

        ValidatorDto result = userAddressServiceValidator.isRequestValidToRetrieveAddressesByMultipleFilters(addressMultipleFiltersRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for null request");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnFalseForNegativePage() {
        addressMultipleFiltersRequest.setPage(-1);

        ValidatorDto result = userAddressServiceValidator.isRequestValidToRetrieveAddressesByMultipleFilters(addressMultipleFiltersRequest, locale);

        assertFalse(result.getSuccess(), "Should return false for negative page number");
    }

    @Test
    public void isRequestValidToRetrieveUsersByMultipleFilters_shouldReturnTrueForValidRequest() {
        ValidatorDto result = userAddressServiceValidator.isRequestValidToRetrieveAddressesByMultipleFilters(addressMultipleFiltersRequest, locale);

        assertTrue(result.getSuccess(), "Should return true for valid request");
    }

    // Tests for isStringValid method

    @Test
    public void isStringValid_shouldReturnFalseForNullString() {
        ValidatorDto result = userAddressServiceValidator.isStringValid(null, locale);

        assertFalse(result.getSuccess(), "Should return false for null string");
    }

    @Test
    public void isStringValid_shouldReturnFalseForEmptyString() {
        ValidatorDto result = userAddressServiceValidator.isStringValid("", locale);

        assertFalse(result.getSuccess(), "Should return false for empty string");
    }

    @Test
    public void isStringValid_shouldReturnFalseForBlankString() {
        ValidatorDto result = userAddressServiceValidator.isStringValid("   ", locale);

        assertFalse(result.getSuccess(), "Should return false for blank string");
    }

    @Test
    public void isStringValid_shouldReturnTrueForValidString() {
        ValidatorDto result = userAddressServiceValidator.isStringValid("Valid String", locale);

        assertTrue(result.getSuccess(), "Should return true for valid string");
    }
}
