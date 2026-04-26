package projectlx.user.management.business.validator.impl;

import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.utils.constants.Constants;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.globalvalidators.Validators;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.validator.api.UserServiceValidator;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserRequest;
import projectlx.user.management.utils.requests.EditUserRequest;
import projectlx.user.management.utils.requests.UsersMultipleFiltersRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatch;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isValidEmail;
import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isValidGender;
import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isValidInternationalPhoneNumber;
import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isValidNationalIdNumber;
import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isValidPhoneLocalPhoneNumber;

@RequiredArgsConstructor
public class UserServiceValidatorImpl implements UserServiceValidator {
    @Value("${constants.max-image-size:2048KB}")
    private String maxImageSize;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceValidatorImpl.class);
    private final List<String> executableExtensions =
            Arrays.asList("application/x-executable", "application/x-msdos-program");

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateUserRequestValid(CreateUserRequest createUserRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (createUserRequest == null) {
            logger.info("Validation failed: CreateUserRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        // Organization ID is optional; no check needed if provided
        // Validate required personal information
        if (isNullOrEmpty(createUserRequest.getUsername()) ||
                isNullOrEmpty(createUserRequest.getFirstName()) ||
                isNullOrEmpty(createUserRequest.getLastName()) ||
                isNullOrEmpty(createUserRequest.getEmail()) ||
                isNullOrEmpty(createUserRequest.getPhoneNumber()) ||
                isNullOrEmpty(createUserRequest.getPassword())) {
            logger.info("Validation failed: One or more required fields are missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_REQUIRED_FIELDS_MISSING.getCode(), new String[]{}, locale));
        }

        // Validate Identification - Accept if National ID or Passport Number is provided, even without file uploads
        if (!isIdentificationInfoProvided(createUserRequest)) {
            logger.info("Validation failed: No identification information provided");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_NO_IDENTIFICATION_PROVIDED.getCode(), new String[]{}, locale));
        }

        // Validate specific formats
        if (!isValidEmail(createUserRequest.getEmail())) {
            logger.info("Validation failed: Email format is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_EMAIL_FORMAT.getCode(), new String[]{}, locale));
        }

        if (!isValidUserName(createUserRequest.getUsername())) {
            logger.info("Validation failed: Username format is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_USERNAME_FORMAT.getCode(), new String[]{}, locale));
        }

        if (!isValidInternationalPhoneNumber(createUserRequest.getPhoneNumber())) {
            logger.info("Validation failed: Phone number format is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_PHONE_NUMBER_FORMAT.getCode(), new String[]{}, locale));
        }

        if (doesStringHaveDigit(createUserRequest.getFirstName()) || doesStringHaveDigit(createUserRequest.getLastName())) {
            logger.info("Validation failed: First name or last name contains digits");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_NAME_CONTAINS_DIGITS.getCode(), new String[]{}, locale));
        }

        if (!isValidGender(createUserRequest.getGender())) {
            logger.info("Validation failed: Gender is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_GENDER.getCode(), new String[]{}, locale));
        }

        if (!isPasswordValid(createUserRequest.getPassword())) {
            logger.info("Validation failed: Password does not meet requirements");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_INVALID.getCode(), new String[]{}, locale));
        }

        if (!isValidDateOfBirth(createUserRequest.getDateOfBirth())) {
            logger.info("Validation failed: Date of birth is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_DATE_OF_BIRTH.getCode(), new String[]{}, locale));
        }

        // File upload validation if files are provided (optional for organization-created users)
        if (createUserRequest.getNationalIdUpload() != null && !createUserRequest.getNationalIdUpload().isEmpty()) {
            try {
                if (!isImageValid(createUserRequest.getNationalIdUpload())) {
                    errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_IMAGE_UPLOAD.getCode(), new String[]{}, locale));
                }
            } catch (IOException e) {
                logger.info("Error while validating National ID upload: {}", e.getMessage());
                errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_IMAGE_UPLOAD.getCode(), new String[]{}, locale));
            }
        }

        if (createUserRequest.getPassportUpload() != null && !createUserRequest.getPassportUpload().isEmpty()) {
            try {
                if (!isImageValid(createUserRequest.getPassportUpload())) {
                    errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_IMAGE_UPLOAD.getCode(), new String[]{}, locale));
                }
            } catch (IOException e) {
                logger.info("Error while validating Passport upload: {}", e.getMessage());
                errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_IMAGE_UPLOAD.getCode(), new String[]{}, locale));
            }
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public boolean isCreateUserRequestValid(CreateUserRequest createUserRequest) {
        return isCreateUserRequestValid(createUserRequest, Locale.getDefault()).getSuccess();
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (id == null || id < 1) {
            logger.info("Validation failed: ID is null or less than 1");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ID_IS_NULL_OR_LESS_THAN_ONE.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public boolean isIdValid(Long id) {
        return isIdValid(id, Locale.getDefault()).getSuccess();
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditUserRequest editUserRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (editUserRequest == null) {
            logger.info("Validation failed: EditUserRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_USER_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (editUserRequest.getId() < 1L) {
            logger.info("Validation failed: User ID is less than 1");
            errors.add(messageService.getMessage(I18Code.MESSAGE_EDIT_USER_ID_LESS_THAN_ONE.getCode(), new String[]{}, locale));
        }

        // Validate required personal information
        if (isNullOrEmpty(editUserRequest.getUsername()) ||
                isNullOrEmpty(editUserRequest.getFirstName()) ||
                isNullOrEmpty(editUserRequest.getLastName()) ||
                isNullOrEmpty(editUserRequest.getEmail()) ||
                isNullOrEmpty(editUserRequest.getPhoneNumber())) {
            logger.info("Validation failed: One or more required fields are missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_REQUIRED_FIELDS_MISSING.getCode(), new String[]{}, locale));
        }

        // Validate identification — must provide at least one complete set (National ID or Passport)
        if (!isUpdateIdentificationProvided(editUserRequest)) {
            logger.info("Validation failed: No valid identification provided");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_NO_IDENTIFICATION_PROVIDED.getCode(), new String[]{}, locale));
        }

        // Validate specific formats and content
        if (!isValidNationalIdNumber(editUserRequest.getNationalIdNumber())) {
            logger.info("Validation failed: National ID number is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_EMAIL_FORMAT.getCode(), new String[]{}, locale));
        }

        if (!isValidEmail(editUserRequest.getEmail())) {
            logger.info("Validation failed: Email format is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_EMAIL_FORMAT.getCode(), new String[]{}, locale));
        }

        if (!isValidUserName(editUserRequest.getUsername())) {
            logger.info("Validation failed: Username format is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_USERNAME_FORMAT.getCode(), new String[]{}, locale));
        }

        if (!isValidInternationalPhoneNumber(editUserRequest.getPhoneNumber())) {
            logger.info("Validation failed: Phone number format is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_PHONE_NUMBER_FORMAT.getCode(), new String[]{}, locale));
        }

        if (doesStringHaveDigit(editUserRequest.getFirstName()) || doesStringHaveDigit(editUserRequest.getLastName())) {
            logger.info("Validation failed: First name or last name contains digits");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_NAME_CONTAINS_DIGITS.getCode(), new String[]{}, locale));
        }

        if (!isValidGender(editUserRequest.getGender())) {
            logger.info("Validation failed: Gender is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_GENDER.getCode(), new String[]{}, locale));
        }

        // Validate Date of Birth
        if (!isValidDateOfBirth(editUserRequest.getDateOfBirth())) {
            logger.info("Validation failed: Date of birth is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_DATE_OF_BIRTH.getCode(), new String[]{}, locale));
        }

        // Validate uploaded image if provided
        if (editUserRequest.getNationalIdUpload() != null && !editUserRequest.getNationalIdUpload().isEmpty()) {
            try {
                if (!isImageValid(editUserRequest.getNationalIdUpload())) {
                    errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_IMAGE_UPLOAD.getCode(), new String[]{}, locale));
                }
            } catch (IOException e) {
                logger.info("Error encountered while validating image: {}", e.getMessage());
                errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_IMAGE_UPLOAD.getCode(), new String[]{}, locale));
            }
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public boolean isRequestValidForEditing(EditUserRequest editUserRequest) {
        return isRequestValidForEditing(editUserRequest, Locale.getDefault()).getSuccess();
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveUsersByMultipleFilters(UsersMultipleFiltersRequest usersMultipleFiltersRequest, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (usersMultipleFiltersRequest == null) {
            logger.info("Validation failed: UsersMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USERS_MULTIPLE_FILTERS_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (usersMultipleFiltersRequest.getPage() < 0) {
            logger.info("Validation failed: Page number is less than 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_PAGE_OUT_OF_BOUNDS.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public boolean isRequestValidToRetrieveUsersByMultipleFilters(UsersMultipleFiltersRequest usersMultipleFiltersRequest) {
        return isRequestValidToRetrieveUsersByMultipleFilters(usersMultipleFiltersRequest, Locale.getDefault()).getSuccess();
    }

    @Override
    public boolean isStringValid(String input) {
        if (input == null || input.trim().isEmpty()) {
            logger.info("Validation failed: String is null or empty");
            return false;
        }
        return true;
    }

    @Override
    public boolean isListValid(List<String> inputList) {
        if (inputList == null || inputList.isEmpty()) {
            logger.info("Validation failed: List is null or empty");
            return false;
        }
        return true;
    }

    @Override
    public boolean isPhoneNumberValid(String phoneNumber) {
        if (!isValidPhoneLocalPhoneNumber(phoneNumber)) {
            logger.info("Validation failed: Phone number is invalid");
            return false;
        }
        return true;
    }

    @Override
    public boolean isNationalIdValid(String nationalIdNumber) {
        if (!isValidNationalIdNumber(nationalIdNumber)) {
            logger.info("Validation failed: National ID number is invalid");
            return false;
        }
        return true;
    }

    @Override
    public boolean isValidUserName(String name)
    {
        if (name == null) {
            logger.info("Validation failed: Username is null");
            return false;
        }

        String regex = Constants.USERNAME_REGEX;

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(name);

        if (!m.matches()) {
            logger.info("Validation failed: Username format is invalid");
            return false;
        }
        return true;
    }

    @Override
    public boolean isPasswordValid(String password)
    {
        if (password == null) {
            logger.info("Validation failed: Password is null");
            return false;
        }

        String regex = Constants.PASSWORD_REGEX;

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(password);

        if (!m.matches()) {
            logger.info("Validation failed: Password does not meet requirements");
            return false;
        }
        return true;
    }

    static boolean doesStringHaveDigit(String input) {
        if (input == null) {
            return false;
        }

        for (char c : input.toCharArray()) {

            if (Character.isDigit(c)) {
                return true;
            }
        }

        return false;
    }

    private boolean isImageValid(MultipartFile multipartFile) throws IOException {

        if (multipartFile == null) {
            return false;
        }

        byte[] file = multipartFile.getBytes();

        return !(checkImageSizeLimit(file) || checkIfFileTypeAndExtensionAreValid(file));
    }

    private Boolean checkImageSizeLimit(byte[] fileData) {
        return resolveMaxImageSizeBytes() <= fileData.length;
    }

    private Boolean checkIfFileTypeAndExtensionAreValid(byte[] fileData) {

        try {
            MagicMatch match = Magic.getMagicMatch(fileData, false);

            if (executableExtensions.contains(match.getExtension().toUpperCase())) {
                return true;
            }

        } catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
            return false;
        }
        return false;
    }

    private long resolveMaxImageSizeBytes() {
        try {
            return DataSize.parse(maxImageSize).toBytes();
        } catch (Exception ignored) {
            // Keep a safe fallback if external config is missing or malformed.
            return DataSize.ofKilobytes(2048).toBytes();
        }
    }

    public boolean isIdentificationProvided(CreateUserRequest request) {
        boolean hasValidNationalId = StringUtils.hasText(request.getNationalIdNumber())
                && request.getNationalIdUpload() != null
                && !request.getNationalIdUpload().isEmpty();

        boolean hasValidPassport = StringUtils.hasText(request.getPassportNumber())
                && request.getPassportUpload() != null
                && !request.getPassportUpload().isEmpty();

        // Return true if at least one is valid (or both)
        return hasValidNationalId || hasValidPassport;
    }

    public boolean isUpdateIdentificationProvided(EditUserRequest request) {

        boolean hasNationalIdNumber = StringUtils.hasText(request.getNationalIdNumber());
        boolean hasNationalIdUpload = request.getNationalIdUpload() != null && !request.getNationalIdUpload().isEmpty();

        boolean hasPassportNumber = StringUtils.hasText(request.getPassportNumber());
        boolean hasPassportUpload = request.getPassportUpload() != null && !request.getPassportUpload().isEmpty();

        // Either both National ID Number & Upload
        boolean hasValidNationalId = hasNationalIdNumber && hasNationalIdUpload;

        // Or both Passport Number & Upload
        boolean hasValidPassport = hasPassportNumber && hasPassportUpload;

        // Or both provided
        boolean hasBoth = hasValidNationalId && hasValidPassport;

        // At least one valid identification or both
        return hasValidNationalId || hasValidPassport || hasBoth;
    }

    private boolean isValidDateOfBirth(String dateOfBirth) {

        try {

            LocalDate dob = LocalDate.parse(dateOfBirth);
            return Validators.isAtLeast16YearsOld(dob);

        } catch (Exception ex) {

            logger.info("Error encountered while validating dateOfBirth: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isIdentificationInfoProvided(CreateUserRequest request) {
        boolean hasNationalId = StringUtils.hasText(request.getNationalIdNumber());
        boolean hasPassport = StringUtils.hasText(request.getPassportNumber());
        return hasNationalId || hasPassport;
    }
}
