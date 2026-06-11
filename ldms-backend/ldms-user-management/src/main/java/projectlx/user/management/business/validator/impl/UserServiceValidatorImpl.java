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
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
    @Value("${constants.max-image-size:15MB}")
    private String maxImageSize;
    private static final Logger logger = LoggerFactory.getLogger(UserServiceValidatorImpl.class);

    private static final Set<String> ALLOWED_IDENTIFICATION_MIME_BASE = Set.of(
            "application/pdf",
            "application/x-pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/heic",
            "image/heif",
            "image/tiff"
    );

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

        // Identification: require at least one of (national id number + document) or (passport number + document).
        // Document = non-empty multipart or an existing upload id (pre-staged file).
        if (!hasAnyIdentificationNumber(createUserRequest)) {
            logger.info("Validation failed: No identification number provided");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_NO_IDENTIFICATION_PROVIDED.getCode(), new String[]{}, locale));
        } else if (!isIdentificationWithDocumentComplete(createUserRequest)) {
            logger.info("Validation failed: Identification number without matching document");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_IDENTIFICATION_DOCUMENT_MISSING.getCode(), new String[]{}, locale));
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

        appendIdentificationUploadErrors(createUserRequest.getNationalIdUpload(), locale, errors);
        appendIdentificationUploadErrors(createUserRequest.getPassportUpload(), locale, errors);

        if (Boolean.TRUE.equals(createUserRequest.getOrganizationKycApprover())) {
            if (createUserRequest.getOrganizationId() != null || createUserRequest.getBranchId() != null) {
                logger.info("Validation failed: KYC approver cannot be linked to an organisation or branch");
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_CREATE_USER_KYC_APPROVER_REQUIRES_NO_ORG.getCode(), new String[]{}, locale));
            }
        }

        if (Boolean.TRUE.equals(createUserRequest.getOperationalIssueHandler())) {
            if (createUserRequest.getOrganizationId() != null || createUserRequest.getBranchId() != null) {
                logger.info("Validation failed: operational issue handler cannot be linked to an organisation or branch");
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_CREATE_USER_OPERATIONAL_HANDLER_REQUIRES_NO_ORG.getCode(), new String[]{}, locale));
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

        // Validate specific formats and content (only when a value was supplied for that field)
        if (StringUtils.hasText(editUserRequest.getNationalIdNumber())
                && !isValidNationalIdNumber(editUserRequest.getNationalIdNumber())) {
            logger.info("Validation failed: National ID number is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_NATIONAL_ID_INVALID.getCode(), new String[]{}, locale));
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

        if (!isValidUserManagementGender(editUserRequest.getGender())) {
            logger.info("Validation failed: Gender is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_GENDER.getCode(), new String[]{}, locale));
        }

        // Validate Date of Birth
        if (!isValidDateOfBirth(editUserRequest.getDateOfBirth())) {
            logger.info("Validation failed: Date of birth is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_DATE_OF_BIRTH.getCode(), new String[]{}, locale));
        }

        appendIdentificationUploadErrors(editUserRequest.getNationalIdUpload(), locale, errors);
        appendIdentificationUploadErrors(editUserRequest.getPassportUpload(), locale, errors);

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
        if (!StringUtils.hasText(phoneNumber)) {
            return false;
        }
        if (!isValidPhoneLocalPhoneNumber(phoneNumber)) {
            logger.info("Validation failed: Phone number is invalid");
            return false;
        }
        return true;
    }

    @Override
    public boolean isNationalIdValid(String nationalIdNumber) {
        if (!StringUtils.hasText(nationalIdNumber)) {
            return false;
        }
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

    private void appendIdentificationUploadErrors(MultipartFile multipartFile, Locale locale, List<String> errors) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            return;
        }
        try {
            byte[] fileData = multipartFile.getBytes();
            if (exceedsMaxIdentificationFileSize(fileData)) {
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_CREATE_USER_IDENTIFICATION_FILE_TOO_LARGE.getCode(),
                        new String[]{humanReadableMaxImageSize()},
                        locale));
            } else if (!isAllowedIdentificationFile(fileData, multipartFile.getContentType())) {
                errors.add(messageService.getMessage(
                        I18Code.MESSAGE_CREATE_USER_IDENTIFICATION_FILE_TYPE_NOT_ALLOWED.getCode(),
                        new String[]{},
                        locale));
            }
        } catch (IOException e) {
            logger.info("Error while validating identification upload: {}", e.getMessage());
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_IMAGE_UPLOAD.getCode(),
                    new String[]{}, locale));
        }
    }

    private boolean exceedsMaxIdentificationFileSize(byte[] fileData) {
        return fileData.length > resolveMaxImageSizeBytes();
    }

    private String humanReadableMaxImageSize() {
        return DataSize.ofBytes(resolveMaxImageSizeBytes()).toString();
    }

    private boolean isAllowedIdentificationFile(byte[] fileData, String declaredContentType) {
        Optional<String> mimeOpt = normalizedMime(fileData, declaredContentType);
        if (mimeOpt.isEmpty()) {
            return false;
        }
        String mimeBase = mimeOpt.get();
        if (mimeLooksDangerous(mimeBase)) {
            return false;
        }
        return ALLOWED_IDENTIFICATION_MIME_BASE.contains(mimeBase);
    }

    private Optional<String> normalizedMime(byte[] fileData, String declaredContentType) {
        return Validators.sniffMimeBase(fileData, declaredContentType);
    }

    private static boolean mimeLooksDangerous(String mimeBase) {
        String m = mimeBase.toLowerCase(Locale.ROOT);
        return m.contains("executable")
                || m.contains("x-msdownload")
                || m.contains("x-dosexec")
                || m.contains("x-msdos")
                || m.contains("java-archive");
    }

    private long resolveMaxImageSizeBytes() {
        try {
            return DataSize.parse(maxImageSize).toBytes();
        } catch (Exception ignored) {
            // Keep a safe fallback if external config is missing or malformed.
            return DataSize.ofMegabytes(15).toBytes();
        }
    }

    public boolean isIdentificationProvided(CreateUserRequest request) {
        return hasAnyIdentificationNumber(request) && isIdentificationWithDocumentComplete(request);
    }

    public boolean isUpdateIdentificationProvided(EditUserRequest request) {

        boolean hasNationalIdNumber = StringUtils.hasText(request.getNationalIdNumber());
        boolean hasNationalIdUpload = request.getNationalIdUpload() != null && !request.getNationalIdUpload().isEmpty();
        boolean hasNationalIdUploadId = request.getNationalIdUploadId() != null && request.getNationalIdUploadId() > 0L;

        boolean hasPassportNumber = StringUtils.hasText(request.getPassportNumber());
        boolean hasPassportUpload = request.getPassportUpload() != null && !request.getPassportUpload().isEmpty();
        boolean hasPassportUploadId = request.getPassportUploadId() != null && request.getPassportUploadId() > 0L;

        // National ID number plus a new upload or an existing file-upload reference.
        boolean hasValidNationalId = hasNationalIdNumber && (hasNationalIdUpload || hasNationalIdUploadId);

        // Passport number plus a new upload or an existing file-upload reference.
        boolean hasValidPassport = hasPassportNumber && (hasPassportUpload || hasPassportUploadId);

        // Provisional org-contact IDs (e.g. PENDING-2) may exist before a scan is uploaded.
        boolean provisionalNationalId = hasNationalIdNumber
                && request.getNationalIdNumber().trim().toUpperCase(Locale.ROOT).startsWith("PENDING-");

        return hasValidNationalId || hasValidPassport || provisionalNationalId;
    }

    private boolean isValidUserManagementGender(String gender) {
        if (!StringUtils.hasText(gender)) {
            return false;
        }
        try {
            projectlx.user.management.model.Gender.valueOf(gender.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isValidDateOfBirth(String dateOfBirth) {

        try {

            LocalDate dob = LocalDate.parse(dateOfBirth);
            return Validators.isAtLeast18YearsOld(dob);

        } catch (Exception ex) {

            logger.info("Error encountered while validating dateOfBirth: {}", ex.getMessage());
            return false;
        }
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean hasNonEmptyMultipart(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private static boolean hasAnyIdentificationNumber(CreateUserRequest request) {
        return StringUtils.hasText(request.getNationalIdNumber())
                || StringUtils.hasText(request.getPassportNumber());
    }

    /**
     * True when at least one identification path is complete: number + (multipart upload or existing file id).
     */
    private static boolean isIdentificationWithDocumentComplete(CreateUserRequest request) {
        boolean nationalComplete = StringUtils.hasText(request.getNationalIdNumber())
                && (hasNonEmptyMultipart(request.getNationalIdUpload())
                || request.getNationalIdUploadId() != null);
        boolean passportComplete = StringUtils.hasText(request.getPassportNumber())
                && (hasNonEmptyMultipart(request.getPassportUpload())
                || request.getPassportUploadId() != null);
        return nationalComplete || passportComplete;
    }
}
