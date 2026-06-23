package projectlx.user.management.business.logic.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.DocumentException;
import projectlx.co.zw.shared_library.utils.export.LdmsExportReport;
import projectlx.co.zw.shared_library.utils.export.LdmsPdfReportWriter;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.Objects;

import projectlx.co.zw.shared_library.business.logic.impl.TokenService;
import projectlx.co.zw.shared_library.utils.dtos.FileUploadDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.FileType;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.requests.EditFileUploadRequest;
import projectlx.co.zw.shared_library.utils.requests.FileUploadRequest;
import projectlx.co.zw.shared_library.utils.requests.SingleFileUploadRequest;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;
import projectlx.user.management.business.auditable.api.UserAccountServiceAuditable;
import projectlx.user.management.business.auditable.api.UserPasswordServiceAuditable;
import projectlx.user.management.business.auditable.api.UserPreferencesServiceAuditable;
import projectlx.user.management.business.auditable.api.UserSecurityServiceAuditable;
import projectlx.user.management.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.business.logic.api.UserAccountService;
import projectlx.user.management.business.logic.api.UserAddressService;
import projectlx.user.management.business.logic.api.UserPasswordService;
import projectlx.user.management.business.logic.api.UserPreferencesService;
import projectlx.user.management.business.logic.api.UserSecurityService;
import projectlx.user.management.business.logic.api.UserService;
import projectlx.user.management.business.logic.api.UserTypeService;
import projectlx.user.management.business.logic.support.OrganizationWorkspaceAccessSupport;
import projectlx.user.management.business.logic.support.PhoneVerificationSupport;
import projectlx.user.management.business.logic.support.SmsDeliveryDisabledException;
import projectlx.user.management.utils.support.OrganizationPortalRolePolicy;
import projectlx.user.management.model.OtpType;
import projectlx.user.management.business.validator.api.UserServiceValidator;
import projectlx.user.management.clients.FileUploadServiceClient;
import projectlx.user.management.model.Address;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.Gender;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserAccount;
import projectlx.user.management.model.UserAddressDetails;
import projectlx.user.management.model.UserPassword;
import projectlx.user.management.model.UserPreferences;
import projectlx.user.management.model.UserPreferencesDetails;
import projectlx.user.management.model.UserSecurity;
import projectlx.user.management.model.UserSecurityDetails;
import projectlx.user.management.model.UserType;
import projectlx.user.management.model.UserTypeDetails;
import projectlx.user.management.model.UserGroup;
import projectlx.user.management.model.UserRole;
import projectlx.user.management.repository.UserAccountRepository;
import projectlx.user.management.repository.UserAddressRepository;
import projectlx.user.management.repository.UserPasswordRepository;
import projectlx.user.management.repository.UserPreferencesRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.repository.UserSecurityRepository;
import projectlx.user.management.repository.UserTypeRepository;
import projectlx.user.management.repository.specification.UserSpecification;
import projectlx.user.management.utils.dtos.AddressDto;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.dtos.UserAccountDto;
import projectlx.user.management.utils.dtos.UserDto;
import projectlx.user.management.utils.dtos.UserGroupDto;
import projectlx.user.management.utils.dtos.UserPasswordDto;
import projectlx.user.management.utils.dtos.UserPreferencesDto;
import projectlx.user.management.utils.dtos.UserRoleDto;
import projectlx.user.management.utils.dtos.UserSecurityDto;
import projectlx.user.management.utils.dtos.UserTypeDto;
import projectlx.user.management.utils.support.UserRoleDtoModuleEnricher;
import projectlx.user.management.utils.config.EmailVerificationLinkProperties;
import projectlx.user.management.utils.config.PasswordResetLinkProperties;
import projectlx.user.management.utils.notifications.UserNotificationTemplateData;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.co.zw.shared_library.utils.generators.SecureTokenGenerator;
import projectlx.user.management.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.utils.requests.CreateAddressRequest;
import projectlx.user.management.utils.requests.CreateUserPasswordRequest;
import projectlx.user.management.utils.requests.CreateUserPreferencesRequest;
import projectlx.user.management.utils.requests.CreateUserRequest;
import projectlx.user.management.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.utils.requests.EditAddressRequest;
import projectlx.user.management.utils.requests.EditUserRequest;
import projectlx.user.management.utils.requests.ForgotPasswordRequest;
import projectlx.user.management.utils.requests.NotificationRequest;
import projectlx.user.management.utils.requests.UsersMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserAccountResponse;
import projectlx.user.management.utils.responses.AddressResponse;
import projectlx.user.management.utils.responses.UserPreferencesResponse;
import projectlx.user.management.utils.responses.UserResponse;
import projectlx.user.management.utils.responses.UserSecurityResponse;
import projectlx.user.management.utils.responses.UserTypeResponse;
import projectlx.user.management.utils.excel.UserExcelListener;
import projectlx.user.management.utils.excel.UserExcelModel;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserServiceValidator userServiceValidator;
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final UserTypeRepository userTypeRepository;
    private final ModelMapper modelMapper;
    private final UserServiceAuditable userServiceAuditable;
    private final UserAccountServiceAuditable userAccountServiceAuditable;
    private final UserPasswordServiceAuditable userPasswordServiceAuditable;
    private final UserPreferencesServiceAuditable userPreferencesServiceAuditable;
    private final UserSecurityServiceAuditable userSecurityServiceAuditable;
    private final UserAccountService userAccountService;
    private final UserPasswordService userPasswordService;
    private final UserAddressService userAddressService;
    private final UserPreferencesService userPreferencesService;
    private final UserSecurityService userSecurityService;
    private final UserTypeService userTypeService;
    private final FileUploadServiceClient fileUploadServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final TokenService tokenService;
    private final EmailVerificationLinkProperties emailVerificationLinkProperties;
    private final PasswordResetLinkProperties passwordResetLinkProperties;
    private final OrganizationWorkspaceAccessSupport organizationWorkspaceAccessSupport;
    private final PhoneVerificationSupport phoneVerificationSupport;

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String[] HEADERS = {
            "ID", "USERNAME", "EMAIL", "FIRST NAME", "LAST NAME", "GENDER",
            "PHONE NUMBER", "NATIONAL ID", "PASSPORT NUMBER", "DATE OF BIRTH"
    };

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    private String sanitizeCsvValue(String value) {
        // Centralize CSV sanitization rules to satisfy the LDMS gate checks.
        return safe(value);
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest createUserRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userServiceValidator.isCreateUserRequestValid(createUserRequest, locale);

        if (validatorDto == null || !validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_INVALID_REQUEST.getCode(), new String[]{}, locale);

            return buildUserResponseWithErrors(400, false, message, null, null,
                    validatorDto != null ? validatorDto.getErrorMessages() : null);
        }

        boolean isPasswordValid = userServiceValidator.isPasswordValid(createUserRequest.getPassword());

        if (!isPasswordValid) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_INVALID.getCode(), new String[]{}, locale);

            List<String> passwordErrors = new ArrayList<>();
            passwordErrors.add(messageService.getMessage(I18Code.MESSAGE_USER_PASSWORD_INVALID.getCode(), new String[]{},
                    locale));

            return buildUserResponseWithErrors(400, false, message, null, null,
                    passwordErrors);
        }

        Optional<User> userRetrievedByPhone = userRepository.findByPhoneNumberAndEntityStatusNot(
                createUserRequest.getPhoneNumber(), EntityStatus.DELETED);

        if (userRetrievedByPhone.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ALREADY_EXISTS.getCode(), new String[]{}, locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
        }

        Optional<User> userRetrievedByEmail = userRepository.findByEmailAndEntityStatusNot(
                createUserRequest.getEmail(), EntityStatus.DELETED);

        if (userRetrievedByEmail.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ALREADY_EXISTS.getCode(), new String[]{}, locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        User userToBeSaved = modelMapper.map(createUserRequest, User.class);
        applyOrganizationKycApproverFlag(userToBeSaved, createUserRequest);
        applyOperationalIssueHandlerFlag(userToBeSaved, createUserRequest);
        applyProcurementApproverFlag(userToBeSaved, createUserRequest);
        applyShipmentFleetAllocatorFlag(userToBeSaved, createUserRequest);
        applyBillingApproverFlag(userToBeSaved, createUserRequest);

        if (createUserRequest.getUserTypeDetails() != null) {

            Optional<UserType> userTypeOptional = userTypeRepository.findByUserTypeNameAndEntityStatusNot(
                    createUserRequest.getUserTypeDetails().getUserTypeName(), EntityStatus.DELETED);

            UserType resolvedUserType = null;
            if (userTypeOptional.isPresent()) {
                resolvedUserType = userTypeOptional.get();
            } else {

                CreateUserTypeRequest createUserTypeRequest = createUserTypeRequest(createUserRequest.getUserTypeDetails());

                UserTypeResponse userTypeResponse = userTypeService.create(createUserTypeRequest, locale, username);

                if (userTypeResponse.getUserTypeDto() != null && userTypeResponse.getUserTypeDto().getId() != null) {
                    resolvedUserType = userTypeRepository.findByIdAndEntityStatusNot(
                                    userTypeResponse.getUserTypeDto().getId(), EntityStatus.DELETED)
                            .orElse(null);
                }
            }

            if (resolvedUserType != null) {
                userToBeSaved.setUserType(resolvedUserType);
            }
        }

        // === FINAL CORRECTED ADDRESS LOGIC ===
        Address address = null;
        AddressDto addressDtoForResponse = null;

        // SCENARIO 1: An external location ID is provided (integration flow)
        if (createUserRequest.getLocationId() != null) {

            address = userAddressRepository.findByLocationAddressIdAndEntityStatusNot(
                            createUserRequest.getLocationId(), EntityStatus.DELETED)
                    .orElseGet(() -> {
                        Address newLocalAddress = new Address();
                        newLocalAddress.setLocationAddressId(createUserRequest.getLocationId());
                        return userAddressRepository.save(newLocalAddress);
                    });

            if (address != null) {
                addressDtoForResponse = userAddressService.toAddressDto(address);
            }
        }
        // SCENARIO 2: Full address details are provided (standalone flow)
        else if (createUserRequest.getUserAddressDetails() != null) {

            CreateAddressRequest createAddressRequest = convertToLocationServiceRequest(createUserRequest.getUserAddressDetails());

            try {
                AddressResponse addressResponse = userAddressService.create(createAddressRequest, locale, username);

                if (addressResponse.isSuccess() && addressResponse.getAddressDto() != null) {

                    Long newLocationId = addressResponse.getAddressDto().getId(); // This will now correctly be 19

                    // Find the local reference that userAddressService just created.
                    Optional<Address> localAddressOptional = userAddressRepository.findByLocationAddressIdAndEntityStatusNot(
                            newLocationId, EntityStatus.DELETED);

                    if(localAddressOptional.isPresent()){
                        address = localAddressOptional.get();
                        addressDtoForResponse = addressResponse.getAddressDto();
                    } else {
                        logger.error("FATAL: userAddressService created an external address but failed to create the local" +
                                " reference for locationId: {}", newLocationId);
                    }
                } else {
                    logger.warn("Failed to create address in Location Service: {}", addressResponse.getMessage());
                }
            } catch (Exception e) {
                logger.error("Error during address creation process: ", e);
            }
        }

        if (address != null) {

            userToBeSaved.setAddress(address);
            logger.info("Successfully associated address (local ID: {}) with user.", address.getId());
        } else {
            logger.warn("Address object was null. Could not associate an address with the user.");
        }
        // === END OF CORRECTED ADDRESS LOGIC ===

        if (createUserRequest.getUserAddressDetails() != null && address == null) {
            message = messageService.getMessage(I18Code.MESSAGE_ADDRESS_CREATION_FAILED.getCode(), new String[]{}, locale);
            List<String> addressErrors = new ArrayList<>();
            addressErrors.add(message);
            return buildUserResponseWithErrors(400, false, message, null, null, addressErrors);
        }

        User userSaved = userServiceAuditable.create(userToBeSaved, locale, username);

        CreateUserAccountRequest userAccountToBeCreated = createUserAccountRequest(userSaved.getPhoneNumber());
        userAccountToBeCreated.setUserId(userSaved.getId());
        UserAccountResponse userAccountResponse = userAccountService.create(userAccountToBeCreated, locale, username);

        CreateUserPasswordRequest userPasswordToBeCreated = createUserPasswordRequest(userSaved.getId(),
                createUserRequest.getPassword());

        userPasswordService.create(userPasswordToBeCreated, locale, username);

        UserPreferencesResponse userPreferencesResponse = new UserPreferencesResponse();

        if (createUserRequest.getUserPreferencesDetails() != null) {

            CreateUserPreferencesRequest createUserPreferencesRequest = createUserPreferencesRequest(
                    createUserRequest.getUserPreferencesDetails(), userSaved.getId());

            userPreferencesResponse = userPreferencesService.create(createUserPreferencesRequest, locale, username);

            if (!userPreferencesResponse.isSuccess() || userPreferencesResponse.getUserPreferencesDto() == null) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                message = userPreferencesResponse.getMessage() != null && !userPreferencesResponse.getMessage().isBlank()
                        ? userPreferencesResponse.getMessage()
                        : messageService.getMessage(I18Code.MESSAGE_CREATE_USER_PREFERENCES_INVALID_REQUEST.getCode(),
                        new String[]{}, locale);
                List<String> preferenceErrors = userPreferencesResponse.getErrorMessages() != null
                        ? userPreferencesResponse.getErrorMessages()
                        : List.of(message);
                return buildUserResponseWithErrors(400, false, message, null, null, preferenceErrors);
            }

            userPreferencesRepository.findByIdAndEntityStatusNot(
                            userPreferencesResponse.getUserPreferencesDto().getId(), EntityStatus.DELETED)
                    .ifPresent(userSaved::setUserPreferences);
        }

        UserSecurityDetails securityDetails = createUserRequest.getUserSecurityDetails() != null
                ? createUserRequest.getUserSecurityDetails()
                : defaultUserSecurityDetailsForNewUser();

        UserSecurityResponse userSecurityResponse = attachUserSecurityFromDetails(userSaved, securityDetails, locale,
                username);
        if (!userSecurityResponse.isSuccess()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_SECURITY_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null,
                    userSecurityResponse.getErrorMessages() != null ? userSecurityResponse.getErrorMessages()
                            : new ArrayList<>());
        }

        userSaved = userRepository.findByIdAndEntityStatusNot(userSaved.getId(), EntityStatus.DELETED).orElse(userSaved);

        if (createUserRequest.getNationalIdUploadId() != null) {
            userSaved.setNationalIdUploadId(createUserRequest.getNationalIdUploadId());
        }
        if (createUserRequest.getPassportUploadId() != null) {
            userSaved.setPassportUploadId(createUserRequest.getPassportUploadId());
        }

        boolean nationalMultipart = createUserRequest.getNationalIdUpload() != null
                && !createUserRequest.getNationalIdUpload().isEmpty();
        boolean passportMultipart = createUserRequest.getPassportUpload() != null
                && !createUserRequest.getPassportUpload().isEmpty();

        if (nationalMultipart || passportMultipart) {

            try {

                FileUploadRequest fileUploadRequest = buildFileUploadRequest(createUserRequest, userSaved.getId());
                List<MultipartFile> files = fileUploadRequest.getFilesMetadata().stream().map(SingleFileUploadRequest::getFile).toList();
                List<Map<String, Object>> metadataList = fileUploadRequest.getFilesMetadata().stream()
                        .map(metadata -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("fileType", metadata.getFileType());
                            map.put("expiresAt", metadata.getExpiresAt());
                            return map;
                        }).toList();

                Map<String, Object> requestMap = new HashMap<>();
                requestMap.put("filesMetadata", metadataList);
                requestMap.put("ownerType", fileUploadRequest.getOwnerType());
                requestMap.put("ownerId", fileUploadRequest.getOwnerId());

                String fileUploadRequestJson = objectMapper.writeValueAsString(requestMap);
                FileUploadResponse fileUploadResponse = fileUploadServiceClient.upload(files, fileUploadRequestJson);

                if (!fileUploadResponse.isSuccess()) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_IDENTIFICATION_UPLOAD_FAILED.getCode(),
                            new String[]{}, locale);
                    List<String> uploadErrors = new ArrayList<>();
                    String detail = fileUploadResponse.getMessage();
                    if (detail != null && !detail.isBlank()) {
                        uploadErrors.add(detail);
                    } else {
                        uploadErrors.add(message);
                    }
                    return buildUserResponseWithErrors(400, false, message, null, null, uploadErrors);
                }

                if (fileUploadResponse.getFileUploadDtoList() != null) {

                    for (FileUploadDto uploadedFile : fileUploadResponse.getFileUploadDtoList()) {

                        if (FileType.NATIONAL_ID.name().equals(uploadedFile.getFileType().name())) {
                            userSaved.setNationalIdUploadId(uploadedFile.getId());
                        } else if (FileType.PASSPORT.name().equals(uploadedFile.getFileType().name())) {
                            userSaved.setPassportUploadId(uploadedFile.getId());
                        }
                    }
                } else if (fileUploadResponse.getFileUploadDto() != null) {

                    FileUploadDto uploadedFile = fileUploadResponse.getFileUploadDto();

                    if (FileType.NATIONAL_ID.name().equals(uploadedFile.getFileType().name())) {
                        userSaved.setNationalIdUploadId(uploadedFile.getId());
                    } else if (FileType.PASSPORT.name().equals(uploadedFile.getFileType().name())) {
                        userSaved.setPassportUploadId(uploadedFile.getId());
                    }
                }

                if ((nationalMultipart && userSaved.getNationalIdUploadId() == null)
                        || (passportMultipart && userSaved.getPassportUploadId() == null)) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_IDENTIFICATION_UPLOAD_FAILED.getCode(),
                            new String[]{}, locale);
                    return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
                }
            } catch (Exception e) {
                logger.error("Error while uploading the file for user creation: ", e);
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_IDENTIFICATION_UPLOAD_FAILED.getCode(),
                        new String[]{}, locale);
                return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
            }
        }

        String verificationToken = tokenService.generateEmailVerificationToken();
        userSaved.setVerificationToken(verificationToken);
        userSaved.setEmailVerified(false);
        User userSavedForSecondTime = userServiceAuditable.update(userSaved, locale, username);

        sendVerificationEmail(userSavedForSecondTime, verificationToken);
        sendWelcomeSms(userSavedForSecondTime);

        UserDto userDtoReturned = modelMapper.map(userSavedForSecondTime, UserDto.class);
        userDtoReturned.setUserAccountDto(userAccountResponse.getUserAccountDto());
        userDtoReturned.setAddressDto(addressDtoForResponse);
        userDtoReturned.setUserPreferencesDto(userPreferencesResponse.getUserPreferencesDto());
        userDtoReturned.setUserSecurityDto(userSecurityResponse.getUserSecurityDto());

        message = messageService.getMessage(I18Code.MESSAGE_USER_CREATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildUserResponse(201, true, message, userDtoReturned, null, null);
    }

    @Override
    @Transactional
    public UserResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userServiceValidator.isIdValid(id, locale);

        if(validatorDto == null || !validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
        }

        if (!organizationWorkspaceAccessSupport.canReadUser(username, id)) {
            throw new AccessDeniedException("Not allowed to view this user");
        }

        Optional<User> userRetrieved = userRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

        if (userRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserResponse(404, false, message,null, null,
                    null);
        }

        User user = userRetrieved.get();

        if (user.getAddress() != null) {

            try {
                AddressResponse addressResponse = userAddressService.findById(user.getAddress().getId(), locale, username);
                if (addressResponse.isSuccess() && addressResponse.getAddressDto() != null) {
                    // Update the address details in the user object
                    user.getAddress().setAddressDetails(addressResponse.getAddressDto());
                }
            } catch (Exception e) {
                logger.warn("Failed to populate address details for user {}: {}", id, e.getMessage());
            }
        }

        if (user.getUserSecurity() == null) {
            UserSecurityResponse secBoot = attachUserSecurityFromDetails(user, defaultUserSecurityDetailsForNewUser(),
                    locale, username);
            if (!secBoot.isSuccess()) {
                logger.warn("Lazy bootstrap user_security failed for user {} on findById: {}", id, secBoot.getMessage());
            }
            user = userRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(user);
        }

        UserDto userDto = toUserDtoWithRelations(user);

        message = messageService.getMessage(I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserResponse(200, true, message, userDto, null,
                null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username, Locale locale) {
        return findUserByUsernameInternal(username, locale, true);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findCurrentUserForSession(String username, Locale locale) {
        return findUserByUsernameInternal(username, locale, false);
    }

    private UserResponse findUserByUsernameInternal(String username, Locale locale, boolean hydrateRemoteAddress) {

        if (!userServiceValidator.isStringValid(username)) {
            String message = messageService.getMessage(I18Code.MESSAGE_USERNAME_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        Optional<User> userRetrieved = userRepository.findSessionProfileByUsernameIgnoreCaseAndEntityStatusNot(
                username, EntityStatus.DELETED);

        if (userRetrieved.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);
            return buildUserResponse(404, false, message, null, null, null);
        }

        User userReturned = userRetrieved.get();

        if (hydrateRemoteAddress && userReturned.getAddress() != null) {
            try {
                AddressResponse addressResponse = userAddressService.findById(
                        userReturned.getAddress().getId(), locale, username);
                if (addressResponse.isSuccess() && addressResponse.getAddressDto() != null) {
                    userReturned.getAddress().setAddressDetails(addressResponse.getAddressDto());
                }
            } catch (Exception e) {
                logger.warn("Failed to populate address details for username {}: {}", username, e.getMessage());
            }
        }

        UserDto userDto = mapUserEntityToDto(userReturned);

        String message = messageService.getMessage(I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserResponse(200, true, message, userDto, null, null);
    }

    private UserDto mapUserEntityToDto(User userReturned) {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserDto userDto = modelMapper.map(userReturned, UserDto.class);

        if (userReturned.getUserType() != null) {
            userDto.setUserTypeDto(modelMapper.map(userReturned.getUserType(), UserTypeDto.class));
        }

        UserAccount userAccount = userReturned.getUserAccount();
        userDto.setUserAccountDto(
                userAccount != null ? modelMapper.map(userAccount, UserAccountDto.class) : null);

        Address address = userReturned.getAddress();
        userDto.setAddressDto(address != null ? userAddressService.toAddressDto(address) : null);

        UserGroup userGroup = userReturned.getUserGroup();
        if (userGroup != null) {
            userDto.setUserGroupDto(mapUserGroupDto(userGroup));
        }

        UserPassword userPassword = userReturned.getUserPassword();
        userDto.setUserPasswordDto(
                userPassword != null ? modelMapper.map(userPassword, UserPasswordDto.class) : null);

        UserPreferences userPreferences = userReturned.getUserPreferences();
        userDto.setUserPreferencesDto(
                userPreferences != null ? modelMapper.map(userPreferences, UserPreferencesDto.class) : null);

        UserSecurity userSecurity = userReturned.getUserSecurity();
        userDto.setUserSecurityDto(
                userSecurity != null ? modelMapper.map(userSecurity, UserSecurityDto.class) : null);

        return userDto;
    }

    private UserGroupDto mapUserGroupDto(UserGroup userGroup) {
        UserGroupDto userGroupDto = modelMapper.map(userGroup, UserGroupDto.class);
        Set<UserRole> roleSet = userGroup.getUserRoles();
        List<UserRoleDto> roleDtos = new ArrayList<>();
        if (roleSet != null) {
            for (UserRole role : roleSet) {
                if (role.getEntityStatus() != EntityStatus.DELETED) {
                    roleDtos.add(modelMapper.map(role, UserRoleDto.class));
                }
            }
        }
        UserRoleDtoModuleEnricher.enrichAll(roleDtos);
        userGroupDto.setUserRoleDtoSet(roleDtos);
        return userGroupDto;
    }

    @Override
    public UserResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<User> userList = userRepository.findByEntityStatusNot(EntityStatus.DELETED);

        if(userList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildUserResponse(404, false, message, null,
                    null, null);
        }

        List<UserDto> userDtoList = modelMapper.map(userList, new TypeToken<List<UserDto>>(){}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserResponse(200, true, message, null, userDtoList,
                null);
    }

    @Override
    @Transactional
    public UserResponse update(EditUserRequest editUserRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = userServiceValidator.isRequestValidForEditing(editUserRequest, locale);

        if(!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildUserResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<User> userRetrieved = userRepository.findByIdAndEntityStatusNot(editUserRequest.getId(),
                EntityStatus.DELETED);

        if (userRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserResponse(400, false, message, null,null,
                    null);
        }

        User userToBeEdited = userRetrieved.get();
        if (Boolean.TRUE.equals(resolveOrganizationKycApproverFlag(editUserRequest.getOrganizationKycApprover()))
                && (userToBeEdited.getOrganizationId() != null || userToBeEdited.getBranchId() != null)) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_USER_KYC_APPROVER_REQUIRES_NO_ORG.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null,
                    List.of(message));
        }
        if (Boolean.TRUE.equals(resolveBooleanFormFlag(editUserRequest.getProcurementApprover()))
                && organizationWorkspaceAccessSupport.effectiveOrganizationId(userToBeEdited).isEmpty()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_USER_PROCUREMENT_APPROVER_REQUIRES_ORG.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
        }
        if (Boolean.TRUE.equals(resolveBooleanFormFlag(editUserRequest.getShipmentFleetAllocator()))
                && organizationWorkspaceAccessSupport.effectiveOrganizationId(userToBeEdited).isEmpty()) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_USER_SHIPMENT_FLEET_ALLOCATOR_REQUIRES_ORG.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
        }
        String normalizedNewEmail = editUserRequest.getEmail() != null
                ? editUserRequest.getEmail().trim().toLowerCase(Locale.ROOT)
                : "";
        String normalizedPreviousEmail = userToBeEdited.getEmail() != null
                ? userToBeEdited.getEmail().trim().toLowerCase(Locale.ROOT)
                : "";
        boolean emailChanged = org.springframework.util.StringUtils.hasText(normalizedNewEmail)
                && !normalizedNewEmail.equals(normalizedPreviousEmail);

        // ============================================================
        // LOCK: reject email change when email is already verified
        // ============================================================
        if (emailChanged && Boolean.TRUE.equals(userToBeEdited.getEmailVerified())) {
            message = messageService.getMessage(I18Code.MESSAGE_EMAIL_CHANGE_LOCKED.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
        }

        if (emailChanged) {
            Optional<User> emailOwner = userRepository.findByEmailAndEntityStatusNot(normalizedNewEmail, EntityStatus.DELETED);
            if (emailOwner.isPresent() && !emailOwner.get().getId().equals(userToBeEdited.getId())) {
                message = messageService.getMessage(I18Code.MESSAGE_USER_ALREADY_EXISTS.getCode(), new String[]{}, locale);
                return buildUserResponse(400, false, message, null, null, null);
            }
        }
        if (emailChanged) {
            userToBeEdited.setEmail(normalizedNewEmail);
            userToBeEdited.setUsername(normalizedNewEmail);
            userToBeEdited.setEmailVerified(false);
        } else {
            userToBeEdited.setUsername(editUserRequest.getUsername());
            userToBeEdited.setEmail(editUserRequest.getEmail());
        }
        userToBeEdited.setFirstName(editUserRequest.getFirstName());
        userToBeEdited.setLastName(editUserRequest.getLastName());
        Gender gender = safeGender(editUserRequest.getGender()).orElse(userToBeEdited.getGender());
        if (gender == null) {
            gender = Gender.PREFER_NOT_TO_SAY;
        }
        userToBeEdited.setGender(gender);
        userToBeEdited.setNationalIdNumber(editUserRequest.getNationalIdNumber());
        userToBeEdited.setPassportNumber(editUserRequest.getPassportNumber());

        // ============================================================
        // LOCK: reject phone change when phone is already verified
        // ============================================================
        String normalizedNewPhone = editUserRequest.getPhoneNumber() != null
                ? editUserRequest.getPhoneNumber().trim()
                : "";
        String normalizedPreviousPhone = userToBeEdited.getPhoneNumber() != null
                ? userToBeEdited.getPhoneNumber().trim()
                : "";
        boolean phoneChanged = org.springframework.util.StringUtils.hasText(normalizedNewPhone)
                && !normalizedNewPhone.equals(normalizedPreviousPhone);
        if (phoneChanged && Boolean.TRUE.equals(userToBeEdited.getPhoneVerified())) {
            message = messageService.getMessage(I18Code.MESSAGE_PHONE_CHANGE_LOCKED.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
        }

        userToBeEdited.setPhoneNumber(editUserRequest.getPhoneNumber());
        userToBeEdited.setDateOfBirth(Date.valueOf(editUserRequest.getDateOfBirth()));
        syncUserAccountPhoneNumber(userToBeEdited, editUserRequest.getPhoneNumber());

        if (editUserRequest.getNationalIdUploadId() != null) {
            userToBeEdited.setNationalIdUploadId(editUserRequest.getNationalIdUploadId());
        }
        if (editUserRequest.getPassportUploadId() != null) {
            userToBeEdited.setPassportUploadId(editUserRequest.getPassportUploadId());
        }
        applyOrganizationKycApproverFlagOnUpdate(userToBeEdited, editUserRequest);
        applyOperationalIssueHandlerFlagOnUpdate(userToBeEdited, editUserRequest);
        applyProcurementApproverFlagOnUpdate(userToBeEdited, editUserRequest);
        applyShipmentFleetAllocatorFlagOnUpdate(userToBeEdited, editUserRequest);
        applyBillingApproverFlagOnUpdate(userToBeEdited, editUserRequest);

        User userEdited = userServiceAuditable.update(userToBeEdited, locale, username);
        applyUserAddressOnUpdate(userEdited, editUserRequest, locale, username);

        boolean nationalMultipart = editUserRequest.getNationalIdUpload() != null
                && !editUserRequest.getNationalIdUpload().isEmpty();
        boolean passportMultipart = editUserRequest.getPassportUpload() != null
                && !editUserRequest.getPassportUpload().isEmpty();

        if (nationalMultipart || passportMultipart) {
        try {

            EditFileUploadRequest editFileUploadRequest = buildFileUploadUpdateRequest(editUserRequest, userEdited.getId(),
                    userEdited.getNationalIdUploadId(), userEdited.getPassportUploadId());

            logger.info("Incoming request to update file(s) metadata: ownerType={}, ownerId={}, number of files={}",
                    editFileUploadRequest.getOwnerType(),
                    editFileUploadRequest.getOwnerId(),
                    editFileUploadRequest.getFilesMetadata() != null ? editFileUploadRequest.getFilesMetadata().size() : 0
            );

            // Extract the actual files from the metadata
            List<MultipartFile> files = editFileUploadRequest.getFilesMetadata()
                    .stream()
                    .map(SingleFileUploadRequest::getFile)
                    .toList();

            // Build metadata manually without the MultipartFile field
            List<Map<String, Object>> metadataList = editFileUploadRequest.getFilesMetadata().stream()
                    .map(metadata -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", metadata.getId());
                        map.put("fileType", metadata.getFileType());
                        map.put("expiresAt", metadata.getExpiresAt());
                        return map;
                    })
                    .toList();

            // Build the final JSON-ready map
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("filesMetadata", metadataList);
            requestMap.put("ownerType", editFileUploadRequest.getOwnerType());
            requestMap.put("ownerId", editFileUploadRequest.getOwnerId());

            // Convert the map to JSON string
            String editFileUploadRequestJson;
            try {
                editFileUploadRequestJson = objectMapper.writeValueAsString(requestMap);
                logger.info("Serialized editFileUploadRequest JSON: {}", editFileUploadRequestJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to convert editFileUploadRequest metadata to JSON", e);
            }

            // Call the Feign Client with files and JSON metadata
            FileUploadResponse fileUploadResponse = fileUploadServiceClient.update(files, editFileUploadRequestJson);

            logger.info("Outgoing response after updating file(s): {}", fileUploadResponse);

            if (fileUploadResponse.isSuccess() && fileUploadResponse.getFileUploadDto() != null) {

                FileUploadDto uploadedFile = fileUploadResponse.getFileUploadDto();

                if (FileType.NATIONAL_ID.equals(uploadedFile.getFileType())) {
                    userEdited.setNationalIdUploadId(uploadedFile.getId());
                } else if (FileType.PASSPORT.equals(uploadedFile.getFileType())) {
                    userEdited.setPassportUploadId(uploadedFile.getId());
                }

            } else if (fileUploadResponse.isSuccess() && fileUploadResponse.getFileUploadDtoList() != null) {

                for (FileUploadDto uploadedFile : fileUploadResponse.getFileUploadDtoList()) {

                    if (FileType.NATIONAL_ID.equals(uploadedFile.getFileType())) {
                        userEdited.setNationalIdUploadId(uploadedFile.getId());
                    } else if (FileType.PASSPORT.equals(uploadedFile.getFileType())) {
                        userEdited.setPassportUploadId(uploadedFile.getId());
                    }
                }
            }

        } catch (Exception e) {

            logger.error("Error while uploading identification file(s) during user update: ", e);
        }
        }

        User userEditedForTheSecondTime = userServiceAuditable.update(userEdited, locale, username);

        User userForResponse = userRepository.findByIdAndEntityStatusNot(userEdited.getId(), EntityStatus.DELETED)
                .orElse(userEditedForTheSecondTime);
        if (userForResponse.getUserSecurity() == null) {
            UserSecurityResponse bootstrap = attachUserSecurityFromDetails(userForResponse,
                    defaultUserSecurityDetailsForNewUser(), locale, username);
            if (!bootstrap.isSuccess()) {
                logger.warn("Could not bootstrap user_security for user {} after update: {}", userForResponse.getId(),
                        bootstrap.getMessage());
            }
            userForResponse = userRepository.findByIdAndEntityStatusNot(userEdited.getId(), EntityStatus.DELETED)
                    .orElse(userForResponse);
        }

        if (emailChanged) {
            String verificationToken = tokenService.generateEmailVerificationToken();
            userForResponse.setVerificationToken(verificationToken);
            userForResponse.setEmailVerified(false);
            User userPendingVerification = userServiceAuditable.update(userForResponse, locale, username);
            sendVerificationEmail(userPendingVerification, verificationToken);
            userForResponse = userRepository.findByIdAndEntityStatusNot(userEdited.getId(), EntityStatus.DELETED)
                    .orElse(userPendingVerification);
        }

        UserDto userDtoReturned = toUserDtoWithRelations(userForResponse);

        message = messageService.getMessage(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserResponse(201, true, message, userDtoReturned, null,
                null);
    }

    private void syncUserAccountPhoneNumber(User user, String phoneNumber) {
        if (user == null || user.getId() == null || !org.springframework.util.StringUtils.hasText(phoneNumber)) {
            return;
        }
        userAccountRepository.findByUser_IdAndEntityStatusNot(user.getId(), EntityStatus.DELETED).ifPresent(account -> {
            account.setPhoneNumber(phoneNumber.trim());
            userAccountRepository.save(account);
        });
    }

    private void applyUserAddressOnUpdate(User user, EditUserRequest editUserRequest, Locale locale, String username) {
        UserAddressDetails details = editUserRequest.getUserAddressDetails();
        if (details == null || user == null || user.getId() == null) {
            return;
        }
        if (!org.springframework.util.StringUtils.hasText(details.getLine1())
                || !org.springframework.util.StringUtils.hasText(details.getPostalCode())
                || details.getSuburbId() == null) {
            return;
        }

        Address existing = user.getAddress();
        if (existing == null) {
            existing = userRepository.findByIdAndEntityStatusNot(user.getId(), EntityStatus.DELETED)
                    .map(User::getAddress)
                    .orElse(null);
        }

        if (existing != null && existing.getId() != null) {
            EditAddressRequest editAddressRequest = new EditAddressRequest();
            editAddressRequest.setId(existing.getId());
            if (existing.getLocationAddressId() != null) {
                editAddressRequest.setLocationAddressId(existing.getLocationAddressId());
            }
            editAddressRequest.setLine1(details.getLine1().trim());
            editAddressRequest.setLine2(details.getLine2());
            editAddressRequest.setPostalCode(details.getPostalCode().trim());
            editAddressRequest.setSuburbId(details.getSuburbId());
            editAddressRequest.setGeoCoordinatesId(details.getGeoCoordinatesId());
            AddressResponse addressResponse = userAddressService.update(editAddressRequest, username, locale);
            if (!addressResponse.isSuccess()) {
                logger.warn("Could not update address for user {}: {}", user.getId(), addressResponse.getMessage());
            }
            return;
        }

        CreateAddressRequest createAddressRequest = convertToLocationServiceRequest(details);
        try {
            AddressResponse addressResponse = userAddressService.create(createAddressRequest, locale, username);
            if (!addressResponse.isSuccess() || addressResponse.getAddressDto() == null) {
                logger.warn("Could not create address for user {}: {}", user.getId(), addressResponse.getMessage());
                return;
            }
            Long locationAddressId = addressResponse.getAddressDto().getId();
            Optional<Address> localAddress = userAddressRepository.findByLocationAddressIdAndEntityStatusNot(
                    locationAddressId, EntityStatus.DELETED);
            if (localAddress.isPresent()) {
                user.setAddress(localAddress.get());
                userRepository.save(user);
            } else {
                logger.warn("Address created in location service but local reference missing for user {}", user.getId());
            }
        } catch (Exception ex) {
            logger.error("Error creating address during user update for user {}", user.getId(), ex);
        }
    }

    @Override
    public UserResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userServiceValidator.isIdValid(id, locale);

        if (validatorDto == null || !validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildUserResponse(400, false, message,null, null,
                    null);
        }

        Optional<User> userRetrieved = userRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);

        if (userRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserResponse(404, false, message, null, null,
                    null);
        }

        User userToBeDeleted = userRetrieved.get();
        userToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        Optional<UserAccount> userAccountRetrieved = userAccountRepository.findByIdAndEntityStatusNot(
                userToBeDeleted.getUserAccount().getId(), EntityStatus.DELETED);

        if (userAccountRetrieved.isPresent()) {
            UserAccount userAccountDeleted = userAccountServiceAuditable.delete(userToBeDeleted.getUserAccount(),
                    locale, username);
        }

        Optional<UserPassword> userPasswordRetrieved = userPasswordRepository.findByIdAndEntityStatusNot(
                userToBeDeleted.getUserPassword().getId(), EntityStatus.DELETED);

        if (userPasswordRetrieved.isPresent()) {
            UserPassword userPasswordDeleted = userPasswordServiceAuditable.delete(userPasswordRetrieved.get(), locale);
        }

        if (userToBeDeleted.getUserPreferences() != null) {
            Optional<UserPreferences> userPreferencesRetrieved = userPreferencesRepository.findByIdAndEntityStatusNot(
                    userToBeDeleted.getUserPreferences().getId(), EntityStatus.DELETED);

            if (userPreferencesRetrieved.isPresent()) {
                userPreferencesServiceAuditable.delete(userToBeDeleted.getUserPreferences(), locale);
            }
        }

        if (userToBeDeleted.getUserSecurity() != null) {
            Optional<UserSecurity> userSecurityReceived = userSecurityRepository.findByIdAndEntityStatusNot(
                    userToBeDeleted.getUserSecurity().getId(), EntityStatus.DELETED);

            if (userSecurityReceived.isPresent()) {
                userSecurityServiceAuditable.delete(userToBeDeleted.getUserSecurity(), locale);
            }
        }

        User userDeleted = userServiceAuditable.delete(userToBeDeleted, locale);

        UserDto userDtoReturned = modelMapper.map(userDeleted, UserDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserResponse(200, true, message, userDtoReturned, null,
                null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByMultipleFilters(UsersMultipleFiltersRequest usersMultipleFiltersRequest,
                                              String username, Locale locale) {

        String message = "";

        Specification<User> spec = buildInitialUserFilterSpec(usersMultipleFiltersRequest);

        ValidatorDto validatorDto = userServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(
                usersMultipleFiltersRequest, locale);

        if (validatorDto == null || !validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_USER_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
        }

        Pageable pageable = PageRequest.of(usersMultipleFiltersRequest.getPage(),
                usersMultipleFiltersRequest.getSize());

        boolean isFirstNameValid = userServiceValidator.isStringValid(
                usersMultipleFiltersRequest.getFirstName());

        if (isFirstNameValid) {

            spec = addToSpec(usersMultipleFiltersRequest.getFirstName(), spec,
                    UserSpecification::firstNameLike);
        }

        boolean isLastNameValid = userServiceValidator.isStringValid(
                usersMultipleFiltersRequest.getLastName());

        if (isLastNameValid) {

            spec = addToSpec(usersMultipleFiltersRequest.getLastName(), spec,
                    UserSpecification::lastNameLike);
        }

        boolean isUserNameValid = userServiceValidator.isStringValid(usersMultipleFiltersRequest.getUsername());

        if (isUserNameValid) {

            spec = addToSpec(usersMultipleFiltersRequest.getUsername(), spec, UserSpecification::userNameLike);
        }

        boolean isEmailValid = userServiceValidator.isStringValid(usersMultipleFiltersRequest.getEmail());

        if (isEmailValid) {

            spec = addToSpec(usersMultipleFiltersRequest.getEmail(), spec, UserSpecification::emailLike);
        }

        boolean isGenderValid =
                userServiceValidator.isListValid(usersMultipleFiltersRequest.getGender());

        if (isGenderValid) {

            List<Gender> genderList = new ArrayList<>();

            for (String gender : usersMultipleFiltersRequest.getGender()) {
                safeGender(gender).ifPresent(genderList::add);
            }

            spec = addToGenderSpec(genderList, spec, UserSpecification::genderIn);
        }

        boolean isPhoneNumberValid = userServiceValidator.isPhoneNumberValid(usersMultipleFiltersRequest.getPhoneNumber());

        if (isPhoneNumberValid) {

            spec = addToSpec(usersMultipleFiltersRequest.getPhoneNumber(), spec, UserSpecification::phoneNumberLike);
        }

        boolean isNationalIdNumberValid = userServiceValidator.isNationalIdValid(usersMultipleFiltersRequest.getNationalIdNumber());

        if (isNationalIdNumberValid) {

            spec = addToSpec(usersMultipleFiltersRequest.getNationalIdNumber(), spec, UserSpecification::nationalIdNumberLike);
        }

        boolean isPassportNumberValid = userServiceValidator.isStringValid(usersMultipleFiltersRequest.getPassportNumber());

        if (isPassportNumberValid) {

            spec = addToSpec(usersMultipleFiltersRequest.getPassportNumber(), spec, UserSpecification::passportNumberLike);
        }

        boolean isSearchValueValid = userServiceValidator.isStringValid(usersMultipleFiltersRequest.getSearchValue());

        if (isSearchValueValid) {

            spec = addToSpec(usersMultipleFiltersRequest.getSearchValue(), spec, UserSpecification::any);
        }

        Long filterUserGroupId = usersMultipleFiltersRequest.getUserGroupId();
        if (filterUserGroupId != null && filterUserGroupId > 0) {
            spec = spec.and(UserSpecification.belongsToUserGroup(filterUserGroupId));
        }

        Long filterOrganizationId = usersMultipleFiltersRequest.getOrganizationId();
        if (filterOrganizationId != null && filterOrganizationId > 0
                && userServiceValidator.isIdValid(filterOrganizationId)) {
            spec = spec.and(UserSpecification.organizationIdEquals(filterOrganizationId));
        }

        Long filterBranchId = usersMultipleFiltersRequest.getBranchId();
        if (filterBranchId != null && filterBranchId > 0 && userServiceValidator.isIdValid(filterBranchId)) {
            spec = spec.and(UserSpecification.branchIdEquals(filterBranchId));
        }

        long totalCount = userRepository.count(spec);

        if (totalCount == 0) {
            message = messageService.getMessage(I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);
            Page<UserDto> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            return buildUserResponse(200, true, message, null, null, emptyPage);
        }

        int pageSize = Math.max(1, usersMultipleFiltersRequest.getSize());
        int maxPage = (int) Math.ceil((double) totalCount / pageSize);

        if (usersMultipleFiltersRequest.getPage() >= maxPage) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_PAGE_OUT_OF_BOUNDS.getCode(), new String[]{}, locale);

            return buildUserResponse(404, false, message, null, null,
                    null);
        }

        Page<User> result = userRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserResponse(404, false, message,null, null,
                    null);
        }

        Page<UserDto> userDtoPage = convertUserEntityToUserDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserResponse(200, true, message, null, null, userDtoPage);
    }

    @Override
    public byte[] exportToCsv(List<UserDto> users) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (UserDto user : users) {
            sb.append(user.getId()).append(",")
                    .append(sanitizeCsvValue(user.getUsername())).append(",")
                    .append(sanitizeCsvValue(user.getEmail())).append(",")
                    .append(sanitizeCsvValue(user.getFirstName())).append(",")
                    .append(sanitizeCsvValue(user.getLastName())).append(",")
                    .append(sanitizeCsvValue(enumToString(user.getGender()))).append(",")
                    .append(sanitizeCsvValue(user.getPhoneNumber())).append(",")
                    .append(sanitizeCsvValue(user.getNationalIdNumber())).append(",")
                    .append(sanitizeCsvValue(user.getPassportNumber())).append(",")
                    .append(user.getDateOfBirth()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<UserDto> users) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Users");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (UserDto user : users) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(safe(user.getUsername()));
            row.createCell(2).setCellValue(safe(user.getEmail()));
            row.createCell(3).setCellValue(safe(user.getFirstName()));
            row.createCell(4).setCellValue(safe(user.getLastName()));
            row.createCell(5).setCellValue(enumToString(user.getGender()));
            row.createCell(6).setCellValue(safe(user.getPhoneNumber()));
            row.createCell(7).setCellValue(safe(user.getNationalIdNumber()));
            row.createCell(8).setCellValue(safe(user.getPassportNumber()));
            row.createCell(9).setCellValue(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<UserDto> users) throws DocumentException {
        List<String[]> rows = new ArrayList<>();
        for (UserDto user : users) {
            rows.add(new String[]{
                    String.valueOf(user.getId()),
                    safe(user.getUsername()),
                    safe(user.getEmail()),
                    safe(user.getFirstName()),
                    safe(user.getLastName()),
                    enumToString(user.getGender()),
                    safe(user.getPhoneNumber()),
                    safe(user.getNationalIdNumber()),
                    safe(user.getPassportNumber()),
                    user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : ""
            });
        }
        return LdmsPdfReportWriter.write(LdmsExportReport.builder()
                .title("Users")
                .reportCode("USR-LST")
                .subtitle("User account listing export")
                .columnHeaders(HEADERS)
                .rows(rows)
                .landscape(true)
                .build());
    }

    @Override
    public ImportSummary importUsersFromCsv(InputStream csvInputStream) throws IOException {

        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withSkipLines(1) // Skip header line
                     .build()) {

            // Read all records
            List<String[]> allRecords = csvReader.readAll();
            total = allRecords.size();

            // Get header row to map column indices
            Map<String, Integer> headerMap = new HashMap<>();
            String[] headers = {"USERNAME", "EMAIL", "FIRST NAME", "LAST NAME", "PHONE NUMBER",
                    "NATIONAL ID", "PASSPORT NUMBER", "DATE OF BIRTH", "GENDER"};

            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i], i);
            }

            int rowNumber = 1; // Start from row 1 (after header)
            for (String[] record : allRecords) {
                rowNumber++;
                try {
                    CreateUserRequest request = new CreateUserRequest();
                    request.setUsername(record[headerMap.get("USERNAME")]);
                    request.setEmail(record[headerMap.get("EMAIL")]);
                    request.setFirstName(record[headerMap.get("FIRST NAME")]);
                    request.setLastName(record[headerMap.get("LAST NAME")]);
                    request.setPhoneNumber(record[headerMap.get("PHONE NUMBER")]);
                    request.setNationalIdNumber(record[headerMap.get("NATIONAL ID")]);
                    request.setPassportNumber(record[headerMap.get("PASSPORT NUMBER")]);
                    request.setDateOfBirth(record[headerMap.get("DATE OF BIRTH")]);
                    request.setGender(record[headerMap.get("GENDER")]);
                    request.setPassword("DefaultPassword@123");

                    UserResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        errors.add("Row " + rowNumber + ": " + response.getMessage());
                    }

                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + rowNumber + ": Unexpected error - " + e.getMessage());
                }
            }
        } catch (CsvException e) {
            throw new IOException("Error processing CSV: " + e.getMessage(), e);
        }

        // Determine status code and success flag based on import results
        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0
                ? "Import completed successfully. " + success + " out of " + total + " users imported."
                : "Import failed. No users were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    /**
     * Validates if the uploaded file is a valid Excel file
     * @param file The MultipartFile to validate
     * @return true if the file is a valid Excel file, false otherwise
     */
    public static boolean isValidExcelFile(MultipartFile file) {
        return Objects.equals(file.getContentType(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Override
    public ImportSummary importUsersFromExcel(InputStream excelInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0;

        // Use EasyExcel to read the Excel file
        UserExcelListener listener = new UserExcelListener();
        com.alibaba.excel.EasyExcel.read(excelInputStream, UserExcelModel.class, listener).sheet().doRead();

        // Get users from the listener
        List<User> users = listener.getUsers();

        int rowNumber = 1; // Start from row 1 (after header)
        for (User user : users) {
            try {
                // Create request from user data
                CreateUserRequest request = new CreateUserRequest();
                request.setUsername(user.getUsername());
                request.setEmail(user.getEmail());
                request.setFirstName(user.getFirstName());
                request.setLastName(user.getLastName());
                request.setPhoneNumber(user.getPhoneNumber());
                request.setNationalIdNumber(user.getNationalIdNumber());
                request.setPassportNumber(user.getPassportNumber());
                request.setDateOfBirth(user.getDateOfBirth() != null ?
                        new SimpleDateFormat("yyyy-MM-dd").format(user.getDateOfBirth()) : null);
                request.setGender(user.getGender() != null ? user.getGender().name() : null);
                request.setPassword("DefaultPassword@123"); // Default password

                // Create user
                UserResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                if (response.isSuccess()) {
                    success++;
                } else {
                    failed++;
                    errors.add("Row " + rowNumber + ": " + response.getMessage());
                }
            } catch (Exception e) {
                failed++;
                errors.add("Row " + rowNumber + ": Unexpected error - " + e.getMessage());
            }
            rowNumber++;
        }

        // Determine status code and success flag based on import results
        int total = users.size();
        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0
                ? "Import completed successfully. " + success + " out of " + total + " users imported."
                : "Import failed. No users were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    @Override
    public UserResponse verifyEmail(String email, String token, Locale locale, String username) {
        String message;

        SecureTokenGenerator.TokenValidationResult validationResult = tokenService.validateVerificationToken(token);

        if (!validationResult.isValid()) {

            if (validationResult.isExpired()) {

                message = messageService.getMessage(I18Code.MESSAGE_VERIFICATION_LINK_EXPIRED.getCode(), new String[] {},
                        locale);

                return buildUserResponse(400, false, message, null, null,
                        null);
            } else {

                message = messageService.getMessage(I18Code.MESSAGE_VERIFICATION_LINK_INVALID.getCode(), new String[] {}, locale);

                return buildUserResponse(400, false, message, null, null, null);
            }
        }

        // Find the user
        Optional<User> userOptional = userRepository.findByEmailAndEntityStatusNot(email, EntityStatus.DELETED);

        if (userOptional.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[] {}, locale);

            return buildUserResponse(400, false, message, null, null, null);
        }

        User user = userOptional.get();

        // Check if the email is already verified
        if (Boolean.TRUE.equals(user.getEmailVerified())) {

            message = messageService.getMessage(I18Code.MESSAGE_EMAIL_ALREADY_VERIFIED.getCode(), new String[] {}, locale);

            UserResponse alreadyVerified = buildUserResponse(200, true, message, null, null, null);
            alreadyVerified.setEmailVerificationOutcome("ALREADY_VERIFIED");
            return alreadyVerified;
        }

        // Check if the token matches the stored token
        if (token == null || !token.equals(user.getVerificationToken())) {

            message = messageService.getMessage(I18Code.MESSAGE_VERIFICATION_LINK_INVALID.getCode(), new String[] {}, locale);

            return buildUserResponse(400, false, message, null, null, null);
        }

        // Update the user's email verification status
        user.setEmailVerified(true);
        user.setVerificationToken(null); // Clear the token for security
        userServiceAuditable.create(user, locale, username);

        message = messageService.getMessage(I18Code.MESSAGE_EMAIL_VERIFIED_SUCCESSFULLY.getCode(), new String[] {}, locale);

        UserResponse verified = buildUserResponse(200, true, message, null, null, null);
        verified.setEmailVerificationOutcome("VERIFIED");
        return verified;
    }

    @Override
    public UserResponse forgotPassword(ForgotPasswordRequest forgotPasswordRequest, String clientPlatform,
            Locale locale) {

        String message;

        // Validate input
        if (forgotPasswordRequest == null ||
                forgotPasswordRequest.getUsernameOrEmail() == null ||
                forgotPasswordRequest.getUsernameOrEmail().trim().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_FORGOT_PASSWORD_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            List<String> errors = new ArrayList<>();
            errors.add(message);
            return buildUserResponseWithErrors(400, false, message, null, null, errors);
        }

        String usernameOrEmail = forgotPasswordRequest.getUsernameOrEmail().trim();

        // Find user by email or username
        Optional<User> userOptional = findUserByUsernameOrEmail(usernameOrEmail);

        if (userOptional.isEmpty()) {
            // For security reasons, don't reveal if user exists or not
            message = messageService.getMessage(I18Code.MESSAGE_FORGOT_PASSWORD_EMAIL_SENT.getCode(),
                    new String[]{}, locale);
            return buildUserResponse(200, true, message, null, null, null);
        }

        User user = userOptional.get();

        // Generate password reset token
        String resetToken = tokenService.generatePasswordResetToken();

        // Set token and expiry (24 hours from now)
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(24));

        // Save user with reset token
        userServiceAuditable.update(user, locale, "SYSTEM");

        // Send password reset notifications (link targets the originating portal when known)
        sendPasswordResetEmail(user, resetToken, clientPlatform);
        sendPasswordResetSms(user, resetToken, clientPlatform);
        sendPasswordResetInAppNotification(user);
        sendPasswordResetWhatsApp(user, resetToken, clientPlatform);

        message = messageService.getMessage(I18Code.MESSAGE_FORGOT_PASSWORD_EMAIL_SENT.getCode(),
                new String[]{}, locale);

        return buildUserResponse(200, true, message, null, null, null);
    }

    @Override
    public UserResponse validateResetToken(String token, String email, Locale locale) {

        String message;

        if (token == null || token.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_RESET_TOKEN_INVALID.getCode(),
                    new String[]{}, locale);

            List<String> errors = new ArrayList<>();
            errors.add(message);
            return buildUserResponseWithErrors(400, false, message, null, null, errors);
        }

        // Validate token format
        SecureTokenGenerator.TokenValidationResult tokenResult = tokenService.validateVerificationToken(token);

        if (!tokenResult.isValid()) {
            if (tokenResult.isExpired()) {
                message = messageService.getMessage(I18Code.MESSAGE_RESET_TOKEN_EXPIRED.getCode(),
                        new String[]{}, locale);
            } else {
                message = messageService.getMessage(I18Code.MESSAGE_RESET_TOKEN_INVALID.getCode(),
                        new String[]{}, locale);
            }

            List<String> errors = new ArrayList<>();
            errors.add(message);
            return buildUserResponseWithErrors(400, false, message, null, null, errors);
        }

        // Find user and validate stored token
        Optional<User> userOptional = userRepository.findByEmailAndEntityStatusNot(email, EntityStatus.DELETED);

        if (userOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_RESET_TOKEN_INVALID.getCode(),
                    new String[]{}, locale);

            List<String> errors = new ArrayList<>();
            errors.add(message);
            return buildUserResponseWithErrors(400, false, message, null, null, errors);
        }

        User user = userOptional.get();

        if (user.getPasswordResetToken() == null ||
                !user.getPasswordResetToken().equals(token) ||
                user.getPasswordResetTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getPasswordResetTokenExpiry())) {

            message = messageService.getMessage(I18Code.MESSAGE_RESET_TOKEN_INVALID.getCode(),
                    new String[]{}, locale);

            List<String> errors = new ArrayList<>();
            errors.add(message);
            return buildUserResponseWithErrors(400, false, message, null, null, errors);
        }

        message = messageService.getMessage(I18Code.MESSAGE_RESET_TOKEN_VALID.getCode(),
                new String[]{}, locale);

        return buildUserResponse(200, true, message, null, null, null);
    }

    @Override
    public UserResponse setOrganizationKycApprover(Long id, boolean enabled, Locale locale, String username) {
        ValidatorDto validation = userServiceValidator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, validation.getErrorMessages());
        }
        User user = userRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElse(null);
        if (user == null) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(404, false, message, null, null, null);
        }
        if (enabled && (user.getOrganizationId() != null || user.getBranchId() != null)) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_USER_KYC_APPROVER_REQUIRES_NO_ORG.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
        }
        user.setOrganizationKycApprover(enabled);
        User saved = userServiceAuditable.update(user, locale, username);
        String message = messageService.getMessage(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, toUserDto(saved), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse listOrganizationKycApprovers(Locale locale) {
        List<User> approvers = userRepository.findByOrganizationKycApproverTrueAndOrganizationIdIsNullAndEntityStatusNot(
                EntityStatus.DELETED);
        List<UserDto> dtos = new ArrayList<>();
        for (User user : approvers) {
            dtos.add(toUserDto(user));
        }
        String message = messageService.getMessage(
                I18Code.MESSAGE_ORGANIZATION_KYC_APPROVERS_RETRIEVED.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, null, dtos, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse listOperationalIssueHandlers(Locale locale) {
        List<User> handlers = userRepository.findByOperationalIssueHandlerTrueAndOrganizationIdIsNullAndEntityStatusNot(
                EntityStatus.DELETED);
        List<UserDto> dtos = new ArrayList<>();
        for (User user : handlers) {
            dtos.add(toUserDto(user));
        }
        String message = messageService.getMessage(
                I18Code.MESSAGE_OPERATIONAL_ISSUE_HANDLERS_RETRIEVED.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, null, dtos, null);
    }

    @Override
    public UserResponse setOperationalIssueHandler(Long id, boolean enabled, Locale locale, String username) {
        User user = userRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED)
                .orElse(null);
        if (user == null) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(404, false, message, null, null, null);
        }
        if (enabled && (user.getOrganizationId() != null || user.getBranchId() != null)) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_USER_OPERATIONAL_HANDLER_REQUIRES_NO_ORG.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
        }
        user.setOperationalIssueHandler(enabled);
        User saved = userServiceAuditable.update(user, locale, username);
        String message = messageService.getMessage(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, toUserDto(saved), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse listProcurementApprovers(Locale locale, String username) {
        Optional<Long> organizationId = organizationWorkspaceAccessSupport.sessionOrganizationId(username);
        if (organizationId.isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_USER_PROCUREMENT_APPROVER_REQUIRES_ORG.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
        }
        List<User> approvers = userRepository.findProcurementApproversByOrganizationWorkspace(
                organizationId.get(), EntityStatus.DELETED);
        List<UserDto> dtos = new ArrayList<>();
        for (User user : approvers) {
            dtos.add(toUserDto(user));
        }
        String message = messageService.getMessage(
                I18Code.MESSAGE_PROCUREMENT_APPROVERS_RETRIEVED.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, null, dtos, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findFleetManagersByOrganization(Long organizationId, Locale locale) {
        if (organizationId == null) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }
        List<String> fleetManagerRoles = List.of(
                "ALLOCATE_SHIPMENT", "VIEW_FLEET_DRIVERS", "ORGANIZATION_ADMINISTRATOR");
        List<User> managers = userRepository.findFleetManagersByOrganizationWorkspace(
                organizationId, fleetManagerRoles, EntityStatus.DELETED);
        List<UserDto> dtos = new ArrayList<>();
        for (User user : managers) {
            dtos.add(toUserDto(user));
        }
        String message = messageService.getMessage(
                I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, null, dtos, null);
    }

    @Override
    public UserResponse setProcurementApprover(Long id, boolean enabled, Locale locale, String username) {
        ValidatorDto validation = userServiceValidator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, validation.getErrorMessages());
        }
        User user = userRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (user == null) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(404, false, message, null, null, null);
        }
        if (!organizationWorkspaceAccessSupport.canReadUser(username, id)) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(404, false, message, null, null, null);
        }
        Optional<Long> targetOrg = organizationWorkspaceAccessSupport.effectiveOrganizationId(user);
        if (enabled && targetOrg.isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_USER_PROCUREMENT_APPROVER_REQUIRES_ORG.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
        }
        if (targetOrg.isPresent()
                && !organizationWorkspaceAccessSupport.canReadOrganization(username, targetOrg.get())) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(404, false, message, null, null, null);
        }
        user.setProcurementApprover(enabled);
        User saved = userServiceAuditable.update(user, locale, username);
        String message = messageService.getMessage(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, toUserDto(saved), null, null);
    }

    @Override
    public UserResponse setShipmentFleetAllocator(Long id, boolean enabled, Locale locale, String username) {
        ValidatorDto validation = userServiceValidator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, validation.getErrorMessages());
        }
        User user = userRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (user == null) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(404, false, message, null, null, null);
        }
        if (!organizationWorkspaceAccessSupport.canReadUser(username, id)) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(404, false, message, null, null, null);
        }
        Optional<Long> targetOrg = organizationWorkspaceAccessSupport.effectiveOrganizationId(user);
        if (enabled && targetOrg.isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_USER_SHIPMENT_FLEET_ALLOCATOR_REQUIRES_ORG.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
        }
        if (targetOrg.isPresent()
                && !organizationWorkspaceAccessSupport.canReadOrganization(username, targetOrg.get())) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(404, false, message, null, null, null);
        }
        user.setShipmentFleetAllocator(enabled);
        User saved = userServiceAuditable.update(user, locale, username);
        String message = messageService.getMessage(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, toUserDto(saved), null, null);
    }

    @Override
    public UserResponse setBillingApprover(Long id, boolean enabled, Locale locale, String username) {
        ValidatorDto validation = userServiceValidator.isIdValid(id, locale);
        if (!validation.getSuccess()) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, validation.getErrorMessages());
        }
        User user = userRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED).orElse(null);
        if (user == null) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(404, false, message, null, null, null);
        }
        if (!organizationWorkspaceAccessSupport.canReadUser(username, id)) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(404, false, message, null, null, null);
        }
        Optional<Long> targetOrg = organizationWorkspaceAccessSupport.effectiveOrganizationId(user);
        if (enabled && targetOrg.isEmpty()) {
            String message = messageService.getMessage(
                    I18Code.MESSAGE_CREATE_USER_BILLING_APPROVER_REQUIRES_ORG.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
        }
        if (targetOrg.isPresent()
                && !organizationWorkspaceAccessSupport.canReadOrganization(username, targetOrg.get())) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(404, false, message, null, null, null);
        }
        user.setBillingApprover(enabled);
        User saved = userServiceAuditable.update(user, locale, username);
        String message = messageService.getMessage(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, toUserDto(saved), null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByPhoneNumberOrEmail(String phoneNumberOrEmail, Locale locale) {

        String message = "";

        boolean isPhoneNumberOrEmailValid = userServiceValidator.isStringValid(phoneNumberOrEmail);

        if(!isPhoneNumberOrEmailValid) {

            message = messageService.getMessage(I18Code.MESSAGE_PHONE_NUMBER_OR_EMAIL_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
        }

        String identifier = phoneNumberOrEmail.trim();

        // Eager-fetch auth/session graph (roles, password, security) — avoids LazyInitializationException.
        Optional<User> userRetrieved = userRepository.findSessionProfileByPhoneNumberAndEntityStatusNot(
                identifier, EntityStatus.DELETED);
        if (userRetrieved.isEmpty()) {
            userRetrieved = userRepository.findSessionProfileByEmailIgnoreCaseAndEntityStatusNot(
                    identifier, EntityStatus.DELETED);
        }

        if (userRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserResponse(404, false, message, null, null,
                    null);
        }

        User userReturned = userRetrieved.get();
        UserDto userDto = mapUserEntityToDto(userReturned);

        message = messageService.getMessage(I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserResponse(200, true, message, userDto, null,
                null);
    }

    /**
     * NEW: Resends a verification link to a user who is not yet verified.
     */
    @Override
    public UserResponse resendVerificationLink(String email, Locale locale, String username) {
        String message;

        // 1. Find the user by their email.
        Optional<User> userOptional = userRepository.findByEmailAndEntityStatusNot(email, EntityStatus.DELETED);

        if (userOptional.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildUserResponse(404, false, message, null, null,
                    null);
        }

        User user = userOptional.get();

        // 2. Check the user's current verification status.
        // If the email is already verified, there's no need to send another link.
        if (Boolean.TRUE.equals(user.getEmailVerified())) {

            message = messageService.getMessage(I18Code.MESSAGE_EMAIL_ALREADY_VERIFIED.getCode(), new String[]{},
                    locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
        }

        LocalDateTime createdAt = user.getCreatedAt();
        if (createdAt != null && !createdAt.isBefore(LocalDateTime.now().minusHours(24))) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_VERIFICATION_RESEND_TOO_EARLY.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(400, false, message, null, null, List.of(message));
        }

        // 3. Generate a new verification token.
        String newVerificationToken = tokenService.generateEmailVerificationToken();

        // 4. Update the user record with the new token.
        user.setVerificationToken(newVerificationToken);
        User updatedUser = userServiceAuditable.update(user, locale, username); // Use update for existing entities

        // 5. Send the new verification email using the existing helper method.
        sendVerificationEmail(updatedUser, newVerificationToken);

        message = messageService.getMessage(I18Code.MESSAGE_VERIFICATION_LINK_SENT_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserResponse(200, true, message, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByOrganizationId(Long organizationId, Locale locale, String username) {

        String message = "";

        boolean isIdValid = userServiceValidator.isIdValid(organizationId);

        if(!isIdValid) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
        }

        if (!organizationWorkspaceAccessSupport.canReadOrganization(username, organizationId)) {
            throw new AccessDeniedException("Not allowed to view users for this organisation");
        }

        List<User> userList = userRepository.findByOrganizationWorkspace(organizationId, EntityStatus.DELETED);
        List<UserDto> userDtoList = mapUsersToDtoList(userList);

        message = messageService.getMessage(I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserResponse(200, true, message, null, userDtoList,
                null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findUsernamesByOrganizationId(Long organizationId, Locale locale, String username) {

        if (!userServiceValidator.isIdValid(organizationId)) {
            String message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        if (!organizationWorkspaceAccessSupport.canReadOrganization(username, organizationId)) {
            throw new AccessDeniedException("Not allowed to view users for this organisation");
        }

        List<String> usernames = userRepository
                .findUsernamesByOrganizationWorkspace(organizationId, EntityStatus.DELETED)
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();

        String message = messageService.getMessage(I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        UserResponse response = buildUserResponse(200, true, message, null, null, null);
        response.setUsernames(usernames);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByBranchId(Long branchId, Locale locale, String username) {

        String message = "";

        boolean isIdValid = userServiceValidator.isIdValid(branchId);

        if(!isIdValid) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
        }

        List<User> userList = userRepository.findByBranchIdAndEntityStatusNot(branchId, EntityStatus.DELETED);
        List<UserDto> userDtoList = mapUsersToDtoList(userList);

        message = messageService.getMessage(I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserResponse(200, true, message, null, userDtoList,
                null);
    }

    private Specification<User> buildInitialUserFilterSpec(UsersMultipleFiltersRequest request) {
        String entityStatusRaw = request != null ? request.getEntityStatus() : null;
        if (StringUtils.hasText(entityStatusRaw)) {
            try {
                EntityStatus filterStatus = EntityStatus.valueOf(entityStatusRaw.trim().toUpperCase());
                return Specification.where(UserSpecification.entityStatusEquals(filterStatus));
            } catch (IllegalArgumentException ignored) {
                // Fall through to default: hide soft-deleted rows.
            }
        }
        return addToSpec(null, UserSpecification::deleted);
    }

    private Specification<User> addToSpec(Specification<User> spec,
                                          Function<EntityStatus, Specification<User>> predicateMethod) {
        Specification<User> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<User> addToSpec(final String aString, Specification<User> spec, Function<String,
            Specification<User>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<User> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<User> addToGenderSpec(final List<Gender> genderList, Specification<User> spec,
                                                Function<List<Gender>, Specification<User>> predicateMethod) {
        if (genderList != null && !genderList.isEmpty()) {
            Specification<User> localSpec = Specification.where(predicateMethod.apply(genderList));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Page<UserDto> convertUserEntityToUserDto(Page<User> userPage) {

        List<User> userList = userPage.getContent();
        List<UserDto> userDtoList = new ArrayList<>();

        for (User user : userPage) {
            userDtoList.add(toUserDtoForList(user));
        }

        int page = userPage.getNumber();
        int size = userPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableUsers = PageRequest.of(page, size);

        return new PageImpl<UserDto>(userDtoList, pageableUsers, userPage.getTotalElements());
    }

    // This method would be called right after newUser is saved
    public void sendWelcomeSms(User newUser) {
        // 1. Prepare the dynamic data for the template
        Map<String, Object> data = UserNotificationTemplateData.forUser(newUser);

        // 2. Prepare the recipient details
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                newUser.getId().toString(),
                null, // No email needed for this template
                newUser.getPhoneNumber(),
                null  // No FCM token needed
        );

        // 3. Construct the full request object
        NotificationRequest notificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "NEW_USER_WELCOME_SMS", // The templateKey we just created
                recipient,
                data,
                null
        );

        // 4. Publish the message to RabbitMQ
        try {
            logger.info("Publishing welcome SMS for user: {}", newUser.getId());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notificationRequest);
        } catch (Exception e) {
            logger.error("Failed to publish welcome SMS for user: {}. Error: {}", newUser.getId(), e.getMessage());
        }
    }

    private CreateAddressRequest convertToLocationServiceRequest(UserAddressDetails userAddressDetails) {
        CreateAddressRequest request = new CreateAddressRequest();
        request.setLine1(userAddressDetails.getLine1());
        request.setLine2(userAddressDetails.getLine2());
        request.setPostalCode(userAddressDetails.getPostalCode());
        request.setSuburbId(userAddressDetails.getSuburbId());
        request.setGeoCoordinatesId(userAddressDetails.getGeoCoordinatesId());
        request.setLatitude(userAddressDetails.getLatitude());
        request.setLongitude(userAddressDetails.getLongitude());

        return request;
    }

    // This method would be called right after newUser is saved
    public void sendWhatsAppVerificationReminder(User newUser) {
        // 1. Prepare the dynamic data for the template
        Map<String, Object> data = UserNotificationTemplateData.forUser(newUser, Map.of(
                "userEmail", newUser.getEmail()
        ));

        //newUser.setPhoneNumber("+14155238886");
        // 2. Prepare the recipient details
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                newUser.getId().toString(),
                null,
                newUser.getPhoneNumber(), // The user's WhatsApp-enabled number
                null
        );

        // 3. Construct the full request object
        NotificationRequest notificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "VERIFICATION_EMAIL_REMINDER_WHATSAPP", // The templateKey we just created
                recipient,
                data,
                null
        );

        // 4. Publish the message to RabbitMQ
        try {
            logger.info("Publishing WhatsApp verification reminder for user: {}", newUser.getId());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notificationRequest);
        } catch (Exception e) {
            logger.error("Failed to publish WhatsApp reminder for user: {}. Error: {}", newUser.getId(), e.getMessage());
        }
    }

    /**
     * Enhanced sendVerificationEmail - Add In-App notification for user registration
     */
    public void sendVerificationEmail(User newUser, String verificationToken) {

        // 1. Link opens the admin portal verify page (GET); the page calls POST /verify-email on the API.
        String verificationLink = emailVerificationLinkProperties.buildVerificationLink(
                verificationToken, newUser.getEmail());

        // 2. Prepare the dynamic data that will replace the placeholders in the template
        Map<String, Object> data = UserNotificationTemplateData.forUser(newUser, Map.of(
                "verificationLink", verificationLink
        ));

        // 3. Prepare the recipient details
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                newUser.getId().toString(),
                newUser.getEmail(),
                null,
                null
        );

        // 4. Send EMAIL notification
        NotificationRequest emailNotificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "USER_REGISTRATION_VERIFICATION", // Email template
                recipient,
                data,
                null
        );

        // 6. Publish notifications to RabbitMQ
        try {
            logger.info("Publishing verification email request for user: {}", newUser.getEmail());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", emailNotificationRequest);
            logger.info("Successfully published verification email request for user: {}", newUser.getEmail());

            // In-app requires an FCM device token; skip when none is available (avoids permanent SKIPPED log rows).
            if (StringUtils.hasText(recipient.getFcmToken())) {
                NotificationRequest inAppNotificationRequest = new NotificationRequest(
                        UUID.randomUUID().toString(),
                        "USER_REGISTRATION_IN_APP",
                        recipient,
                        data,
                        null
                );
                logger.info("Publishing verification in-app notification for user: {}", newUser.getId());
                rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", inAppNotificationRequest);
                logger.info("Successfully published verification in-app notification for user: {}", newUser.getId());
            } else {
                logger.debug("Skipping in-app verification notification for user {}: no FCM token", newUser.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to publish verification notifications for user: {}. Error: {}",
                    newUser.getEmail(), e.getMessage());
        }
    }

    public void sendPasswordResetEmail(User user, String resetToken, String clientPlatform) {
        String portalBase = passwordResetLinkProperties.resolvePortalBaseUrl(clientPlatform, user);
        String resetLink = passwordResetLinkProperties.buildResetLink(portalBase, resetToken, user.getEmail());
        String signInLink = passwordResetLinkProperties.buildSignInUrl(portalBase);

        Map<String, Object> data = UserNotificationTemplateData.forUser(user, Map.of(
                "resetLink", resetLink,
                "signInLink", signInLink
        ));

        // Prepare the recipient details
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                user.getId().toString(),
                user.getEmail(),
                null,
                null
        );

        // Construct the notification request
        NotificationRequest notificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "PASSWORD_RESET_REQUEST", // Template key for password reset
                recipient,
                data,
                null
        );

        // Publish to RabbitMQ
        try {
            logger.info("Publishing password reset email for user: {}", user.getEmail());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notificationRequest);
            logger.info("Successfully published password reset email for user: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Failed to publish password reset email for user: {}. Error: {}",
                    user.getEmail(), e.getMessage());
        }
    }

    /**
     * Send SMS notification for password reset request
     */
    public void sendPasswordResetSms(User user, String resetToken, String clientPlatform) {
        String portalBase = passwordResetLinkProperties.resolvePortalBaseUrl(clientPlatform, user);
        String resetLink = passwordResetLinkProperties.buildResetLink(portalBase, resetToken, user.getEmail());
        Map<String, Object> data = UserNotificationTemplateData.forUser(user, Map.of("resetLink", resetLink));

        // Prepare the recipient details
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                user.getId().toString(),
                null, // No email for SMS
                user.getPhoneNumber(),
                null
        );

        // Construct the notification request
        NotificationRequest notificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "PASSWORD_RESET_REQUEST_SMS", // Template key for SMS
                recipient,
                data,
                null
        );

        // Publish to RabbitMQ
        try {
            logger.info("Publishing password reset SMS for user: {}", user.getPhoneNumber());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notificationRequest);
            logger.info("Successfully published password reset SMS for user: {}", user.getPhoneNumber());
        } catch (Exception e) {
            logger.error("Failed to publish password reset SMS for user: {}. Error: {}",
                    user.getPhoneNumber(), e.getMessage());
        }
    }

    /**
     * Send WhatsApp notification for password reset request
     */
    public void sendPasswordResetWhatsApp(User user, String resetToken, String clientPlatform) {
        String portalBase = passwordResetLinkProperties.resolvePortalBaseUrl(clientPlatform, user);
        String resetLink = passwordResetLinkProperties.buildResetLink(portalBase, resetToken, user.getEmail());
        Map<String, Object> data = UserNotificationTemplateData.forUser(user, Map.of("resetLink", resetLink));

        // Prepare the recipient details
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                user.getId().toString(),
                null, // No email for WhatsApp
                user.getPhoneNumber(), // WhatsApp uses phone number
                null
        );

        // Construct the notification request
        NotificationRequest notificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "PASSWORD_RESET_REQUEST_WHATSAPP", // Template key for WhatsApp
                recipient,
                data,
                null
        );

        // Publish to RabbitMQ
        try {
            logger.info("Publishing password reset WhatsApp for user: {}", user.getPhoneNumber());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notificationRequest);
            logger.info("Successfully published password reset WhatsApp for user: {}", user.getPhoneNumber());
        } catch (Exception e) {
            logger.error("Failed to publish password reset WhatsApp for user: {}. Error: {}",
                    user.getPhoneNumber(), e.getMessage());
        }
    }

    /**
     * Send In-App notification for password reset request
     */
    public void sendPasswordResetInAppNotification(User user) {
        // Prepare the dynamic data
        Map<String, Object> data = UserNotificationTemplateData.forUser(user);

        // Prepare the recipient details
        NotificationRequest.Recipient recipient = new NotificationRequest.Recipient(
                user.getId().toString(),
                user.getEmail(), // In-app notifications are tied to user account
                null,
                null // FCM token would be here if using push notifications
        );

        // Construct the notification request
        NotificationRequest notificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "PASSWORD_RESET_IN_APP", // Template key for In-App
                recipient,
                data,
                null
        );

        // Publish to RabbitMQ
        try {
            logger.info("Publishing password reset in-app notification for user: {}", user.getId());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", notificationRequest);
            logger.info("Successfully published password reset in-app notification for user: {}", user.getId());
        } catch (Exception e) {
            logger.error("Failed to publish password reset in-app notification for user: {}. Error: {}",
                    user.getId(), e.getMessage());
        }
    }

    private Optional<User> findUserByUsernameOrEmail(String usernameOrEmail) {
        // Try email first
        Optional<User> userByEmail = userRepository.findByEmailAndEntityStatusNot(usernameOrEmail, EntityStatus.DELETED);

        if (userByEmail.isPresent()) {
            return userByEmail;
        }

        // Try username
        return userRepository.findByUsernameAndEntityStatusNot(usernameOrEmail, EntityStatus.DELETED);
    }

    private CreateUserAccountRequest createUserAccountRequest(String phoneNumber) {

        CreateUserAccountRequest createUserAccountRequest = new CreateUserAccountRequest();

        createUserAccountRequest.setPhoneNumber(phoneNumber);
        createUserAccountRequest.setIsAccountLocked(false);

        return createUserAccountRequest;
    }

    private CreateUserPasswordRequest createUserPasswordRequest(Long userId,  String password) {

        CreateUserPasswordRequest createUserPasswordRequest = new CreateUserPasswordRequest();

        createUserPasswordRequest.setPassword(password);
        createUserPasswordRequest.setUserId(userId);

        return createUserPasswordRequest;
    }

    private CreateAddressRequest createAddressRequest(UserAddressDetails userAddressDetails) {

        CreateAddressRequest createAddressRequest = new CreateAddressRequest();

        createAddressRequest.setLine1(userAddressDetails.getLine1());
        createAddressRequest.setLine2(userAddressDetails.getLine2());
        createAddressRequest.setPostalCode(userAddressDetails.getPostalCode());
        createAddressRequest.setSuburbId(userAddressDetails.getSuburbId());
        createAddressRequest.setGeoCoordinatesId(userAddressDetails.getGeoCoordinatesId());
        createAddressRequest.setLatitude(userAddressDetails.getLatitude());
        createAddressRequest.setLongitude(userAddressDetails.getLongitude());

        return createAddressRequest;
    }

    private CreateUserPreferencesRequest createUserPreferencesRequest(UserPreferencesDetails userPreferencesDetails,
                                                                      Long userId) {

        CreateUserPreferencesRequest createUserPreferencesRequest = new CreateUserPreferencesRequest();

        createUserPreferencesRequest.setPreferredLanguage(userPreferencesDetails.getPreferredLanguage());
        createUserPreferencesRequest.setTimezone(userPreferencesDetails.getTimezone());
        createUserPreferencesRequest.setUserId(userId);

        return createUserPreferencesRequest;
    }

    /**
     * Bootstrap values when the client omits {@link CreateUserRequest#getUserSecurityDetails()}. Users should replace
     * placeholder answers and TOTP secret via the security edit flow.
     */
    private UserSecurityDetails defaultUserSecurityDetailsForNewUser() {
        UserSecurityDetails d = new UserSecurityDetails();
        d.setSecurityQuestion_1("Please set your first security question (profile).");
        d.setSecurityAnswer_1("TEMP-A1-" + UUID.randomUUID());
        d.setSecurityQuestion_2("Please set your second security question (profile).");
        d.setSecurityAnswer_2("TEMP-A2-" + UUID.randomUUID());
        d.setTwoFactorAuthSecret("TOTP-" + UUID.randomUUID().toString().replace("-", ""));
        d.setIsTwoFactorEnabled(Boolean.FALSE);
        return d;
    }

    private UserSecurityResponse attachUserSecurityFromDetails(User user, UserSecurityDetails details, Locale locale,
            String username) {
        CreateUserSecurityRequest createUserSecurityRequest = createUserSecurityRequest(details, user.getId());
        UserSecurityResponse response = userSecurityService.create(createUserSecurityRequest, locale, username);
        if (response.isSuccess() && response.getUserSecurityDto() != null) {
            userSecurityRepository
                    .findByIdAndEntityStatusNot(response.getUserSecurityDto().getId(), EntityStatus.DELETED)
                    .ifPresent(us -> {
                        user.setUserSecurity(us);
                        userServiceAuditable.update(user, locale, username);
                    });
        }
        return response;
    }

    private CreateUserSecurityRequest createUserSecurityRequest(UserSecurityDetails userSecurityDetails,
                                                                Long userId) {

        CreateUserSecurityRequest createUserSecurityRequest = new CreateUserSecurityRequest();

        createUserSecurityRequest.setSecurityQuestion_1(userSecurityDetails.getSecurityQuestion_1());
        createUserSecurityRequest.setSecurityQuestion_2(userSecurityDetails.getSecurityQuestion_2());
        createUserSecurityRequest.setSecurityAnswer_1(userSecurityDetails.getSecurityAnswer_1());
        createUserSecurityRequest.setSecurityAnswer_2(userSecurityDetails.getSecurityAnswer_2());
        createUserSecurityRequest.setTwoFactorAuthSecret(userSecurityDetails.getTwoFactorAuthSecret());
        createUserSecurityRequest.setIsTwoFactorEnabled(
                userSecurityDetails.getIsTwoFactorEnabled() != null ? userSecurityDetails.getIsTwoFactorEnabled()
                        : Boolean.FALSE);
        createUserSecurityRequest.setUserId(userId);

        return createUserSecurityRequest;
    }

    private CreateUserTypeRequest createUserTypeRequest(UserTypeDetails userTypeDetails) {

        CreateUserTypeRequest createUserTypeRequest = new CreateUserTypeRequest();

        createUserTypeRequest.setUserTypeName(userTypeDetails.getUserTypeName());
        createUserTypeRequest.setDescription(userTypeDetails.getDescription());

        return createUserTypeRequest;
    }

    /**
     * Accepts ISO-8601 date-time or calendar date only (e.g. {@code 2050-11-11}), as sent by HTML date inputs.
     */
    private static LocalDateTime parseExpiryStringToLocalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        }
    }

    private FileUploadRequest buildFileUploadRequest(CreateUserRequest createUserRequest, Long userId) {
        FileUploadRequest fileUploadRequest = new FileUploadRequest();
        List<SingleFileUploadRequest> filesMetadata = new ArrayList<>();

        boolean hasNationalIdUpload = createUserRequest.getNationalIdUpload() != null && !createUserRequest.getNationalIdUpload().isEmpty();
        boolean hasPassportUpload = createUserRequest.getPassportUpload() != null && !createUserRequest.getPassportUpload().isEmpty();

        if (hasNationalIdUpload) {
            SingleFileUploadRequest nationalIdFile = new SingleFileUploadRequest();
            nationalIdFile.setFile(createUserRequest.getNationalIdUpload());
            nationalIdFile.setFileType(String.valueOf(FileType.NATIONAL_ID));

            if (createUserRequest.getNationalIdExpiryDate() != null) {
                nationalIdFile.setExpiresAt(parseExpiryStringToLocalDateTime(createUserRequest.getNationalIdExpiryDate()));
            }

            filesMetadata.add(nationalIdFile);
        }

        if (hasPassportUpload) {

            SingleFileUploadRequest passportFile = new SingleFileUploadRequest();
            passportFile.setFile(createUserRequest.getPassportUpload());
            passportFile.setFileType(String.valueOf(FileType.PASSPORT));

            if (createUserRequest.getPassportExpiryDate() != null) {
                passportFile.setExpiresAt(parseExpiryStringToLocalDateTime(createUserRequest.getPassportExpiryDate()));
            }
            filesMetadata.add(passportFile);
        }

        if (filesMetadata.isEmpty()) {
            throw new IllegalArgumentException("National ID or Passport upload is required");
        }

        fileUploadRequest.setFilesMetadata(filesMetadata);
        fileUploadRequest.setOwnerType(OwnerType.USER.getOwnerType());
        fileUploadRequest.setOwnerId(userId);

        return fileUploadRequest;
    }

    private EditFileUploadRequest buildFileUploadUpdateRequest(EditUserRequest editUserRequest, Long userId,
                                                               Long nationalIdFileId, Long passportFileId) {

        EditFileUploadRequest editFileUploadRequest = new EditFileUploadRequest();
        List<SingleFileUploadRequest> filesMetadata = new ArrayList<>();

        boolean hasNationalIdUpload = editUserRequest.getNationalIdUpload() != null && !editUserRequest.getNationalIdUpload().isEmpty();
        boolean hasPassportUpload = editUserRequest.getPassportUpload() != null && !editUserRequest.getPassportUpload().isEmpty();

        if (hasNationalIdUpload) {
            SingleFileUploadRequest nationalIdFile = new SingleFileUploadRequest();

            if (nationalIdFileId != null) {
                nationalIdFile.setId(nationalIdFileId);
            }

            nationalIdFile.setFile(editUserRequest.getNationalIdUpload());
            nationalIdFile.setFileType(String.valueOf(FileType.NATIONAL_ID));

            if (editUserRequest.getNationalIdExpiryDate() != null) {
                nationalIdFile.setExpiresAt(parseExpiryStringToLocalDateTime(editUserRequest.getNationalIdExpiryDate()));
            }

            filesMetadata.add(nationalIdFile);
        }

        if (hasPassportUpload) {
            SingleFileUploadRequest passportFile = new SingleFileUploadRequest();

            if (passportFileId != null) {
                passportFile.setId(passportFileId);
            }

            passportFile.setFile(editUserRequest.getPassportUpload());
            passportFile.setFileType(String.valueOf(FileType.PASSPORT));

            if (editUserRequest.getPassportExpiryDate() != null) {
                passportFile.setExpiresAt(parseExpiryStringToLocalDateTime(editUserRequest.getPassportExpiryDate()));
            }
            filesMetadata.add(passportFile);
        }

        if (filesMetadata.isEmpty()) {
            throw new IllegalArgumentException("National ID or Passport upload is required");
        }

        editFileUploadRequest.setFilesMetadata(filesMetadata);
        editFileUploadRequest.setOwnerType(OwnerType.USER.getOwnerType());
        editFileUploadRequest.setOwnerId(userId);

        return editFileUploadRequest;
    }

    public static Optional<Gender> safeGender(String input) {
        try {
            return Optional.of(Gender.valueOf(input.trim().toUpperCase()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String enumToString(Enum<?> e) {
        return e != null ? e.name() : "";
    }

    private String getCellValue(Cell cell) {

        if (cell == null) return "";

        String value;

        switch (cell.getCellType()) {

            case STRING:
                value = cell.getStringCellValue();
                break;

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                } else {
                    value = String.valueOf((long) cell.getNumericCellValue());
                }
                break;

            case BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;

            default:
                value = "";
                break;
        }

        return value;
    }

    private Date parseDate(String input) {
        try {
            return (Date) new SimpleDateFormat("yyyy-MM-dd").parse(input);
        } catch (Exception e) {
            return null;
        }
    }

    private Gender parseGender(String input) {
        try {
            return Gender.valueOf(input.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private UserResponse buildUserResponse(int statusCode, boolean isSuccess, String message,
                                           UserDto userDto, List<UserDto> userDtoList,
                                           Page<UserDto> userDtoPage){

        UserResponse userResponse = new UserResponse();
        userResponse.setStatusCode(statusCode);
        userResponse.setSuccess(isSuccess);
        userResponse.setMessage(message);
        userResponse.setUserDto(userDto);
        userResponse.setUserDtoList(userDtoList);
        userResponse.setUserDtoPage(userDtoPage);

        return userResponse;
    }

    private UserResponse buildUserResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                     UserDto userDto, List<UserDto> userDtoList,
                                                     List<String> errorMessages){

        UserResponse userResponse = new UserResponse();
        userResponse.setStatusCode(statusCode);
        userResponse.setSuccess(isSuccess);
        userResponse.setMessage(message);
        userResponse.setUserDto(userDto);
        userResponse.setUserDtoList(userDtoList);
        userResponse.setErrorMessages(errorMessages);

        return userResponse;
    }

    private void applyOrganizationKycApproverFlag(User user, CreateUserRequest request) {
        boolean approver = Boolean.TRUE.equals(request.getOrganizationKycApprover())
                && request.getOrganizationId() == null
                && request.getBranchId() == null;
        user.setOrganizationKycApprover(approver);
    }

    private void applyOperationalIssueHandlerFlag(User user, CreateUserRequest request) {
        boolean handler = Boolean.TRUE.equals(request.getOperationalIssueHandler())
                && request.getOrganizationId() == null
                && request.getBranchId() == null;
        user.setOperationalIssueHandler(handler);
    }

    private void applyProcurementApproverFlag(User user, CreateUserRequest request) {
        boolean approver = Boolean.TRUE.equals(request.getProcurementApprover())
                && request.getOrganizationId() != null;
        user.setProcurementApprover(approver);
    }

    private void applyShipmentFleetAllocatorFlag(User user, CreateUserRequest request) {
        boolean allocator = Boolean.TRUE.equals(request.getShipmentFleetAllocator())
                && request.getOrganizationId() != null;
        user.setShipmentFleetAllocator(allocator);
    }

    private void applyBillingApproverFlag(User user, CreateUserRequest request) {
        boolean approver = Boolean.TRUE.equals(request.getBillingApprover())
                && request.getOrganizationId() != null;
        user.setBillingApprover(approver);
    }

    private void applyOrganizationKycApproverFlagOnUpdate(User user, EditUserRequest request) {
        Boolean flag = resolveOrganizationKycApproverFlag(request.getOrganizationKycApprover());
        if (flag == null) {
            return;
        }
        boolean approver = Boolean.TRUE.equals(flag)
                && user.getOrganizationId() == null
                && user.getBranchId() == null;
        user.setOrganizationKycApprover(approver);
    }

    private void applyOperationalIssueHandlerFlagOnUpdate(User user, EditUserRequest request) {
        Boolean flag = resolveBooleanFormFlag(request.getOperationalIssueHandler());
        if (flag == null) {
            return;
        }
        boolean handler = Boolean.TRUE.equals(flag)
                && user.getOrganizationId() == null
                && user.getBranchId() == null;
        user.setOperationalIssueHandler(handler);
    }

    private void applyProcurementApproverFlagOnUpdate(User user, EditUserRequest request) {
        Boolean flag = resolveBooleanFormFlag(request.getProcurementApprover());
        if (flag == null) {
            return;
        }
        boolean approver = Boolean.TRUE.equals(flag)
                && organizationWorkspaceAccessSupport.effectiveOrganizationId(user).isPresent();
        user.setProcurementApprover(approver);
    }

    private void applyShipmentFleetAllocatorFlagOnUpdate(User user, EditUserRequest request) {
        Boolean flag = resolveBooleanFormFlag(request.getShipmentFleetAllocator());
        if (flag == null) {
            return;
        }
        boolean allocator = Boolean.TRUE.equals(flag)
                && organizationWorkspaceAccessSupport.effectiveOrganizationId(user).isPresent();
        user.setShipmentFleetAllocator(allocator);
    }

    private void applyBillingApproverFlagOnUpdate(User user, EditUserRequest request) {
        Boolean flag = resolveBooleanFormFlag(request.getBillingApprover());
        if (flag == null) {
            return;
        }
        boolean approver = Boolean.TRUE.equals(flag)
                && organizationWorkspaceAccessSupport.effectiveOrganizationId(user).isPresent();
        user.setBillingApprover(approver);
    }

    private Boolean resolveOrganizationKycApproverFlag(String raw) {
        return resolveBooleanFormFlag(raw);
    }

    private Boolean resolveBooleanFormFlag(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return "true".equalsIgnoreCase(raw.trim());
    }

    private List<UserDto> mapUsersToDtoList(List<User> userList) {
        List<UserDto> userDtoList = new ArrayList<>();
        if (userList == null || userList.isEmpty()) {
            return userDtoList;
        }
        for (User user : userList) {
            userDtoList.add(toUserDtoForList(user));
        }
        return userDtoList;
    }

    /** List and organisation-scoped reads — includes group and type for portal tables. */
    private UserDto toUserDtoForList(User user) {
        UserDto userDto = toUserDto(user);
        if (user.getUserType() != null) {
            userDto.setUserTypeDto(modelMapper.map(user.getUserType(), UserTypeDto.class));
        }
        UserGroup userGroup = user.getUserGroup();
        if (userGroup != null) {
            userDto.setUserGroupDto(mapUserGroupDto(userGroup));
        }
        return userDto;
    }

    private UserDto toUserDto(User user) {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserDto dto = modelMapper.map(user, UserDto.class);
        dto.setOrganizationKycApprover(user.isOrganizationKycApprover());
        dto.setOperationalIssueHandler(user.isOperationalIssueHandler());
        dto.setProcurementApprover(user.isProcurementApprover());
        dto.setShipmentFleetAllocator(user.isShipmentFleetAllocator());
        dto.setBillingApprover(user.isBillingApprover());
        dto.setOrganizationWorkspaceAdministrator(hasOrganizationAdministratorRole(user));
        dto.setPhoneVerificationDue(phoneVerificationSupport.isPhoneVerificationDue(user));
        dto.setSmsDeliveryEnabled(phoneVerificationSupport.isSmsDeliveryEnabled());
        return dto;
    }

    private boolean hasOrganizationAdministratorRole(User user) {
        if (user == null || user.getUserGroup() == null || user.getUserGroup().getUserRoles() == null) {
            return false;
        }
        return user.getUserGroup().getUserRoles().stream()
                .filter(role -> role != null && role.getEntityStatus() != EntityStatus.DELETED)
                .anyMatch(role -> OrganizationPortalRolePolicy.ORGANIZATION_ADMINISTRATOR.equals(role.getRole()));
    }

    private UserDto toUserDtoWithRelations(User user) {
        UserDto userDto = toUserDto(user);
        if (user.getUserType() != null) {
            userDto.setUserTypeDto(modelMapper.map(user.getUserType(), UserTypeDto.class));
        }
        UserAccount userAccount = user.getUserAccount();
        userDto.setUserAccountDto(
                userAccount != null ? modelMapper.map(userAccount, UserAccountDto.class) : null);
        Address address = user.getAddress();
        userDto.setAddressDto(address != null ? userAddressService.toAddressDto(address) : null);
        UserGroup userGroup = user.getUserGroup();
        userDto.setUserGroupDto(userGroup != null ? mapUserGroupDto(userGroup) : null);
        UserPassword userPassword = user.getUserPassword();
        userDto.setUserPasswordDto(
                userPassword != null ? modelMapper.map(userPassword, UserPasswordDto.class) : null);
        UserPreferences userPreferences = user.getUserPreferences();
        userDto.setUserPreferencesDto(
                userPreferences != null ? modelMapper.map(userPreferences, UserPreferencesDto.class) : null);
        UserSecurity userSecurity = user.getUserSecurity();
        userDto.setUserSecurityDto(
                userSecurity != null ? modelMapper.map(userSecurity, UserSecurityDto.class) : null);
        return userDto;
    }

    // ============================================================
    //  Phone verification — frontend-facing
    // ============================================================

    @Override
    @Transactional
    public UserResponse requestPhoneVerification(String username, Locale locale) {
        String message;

        Optional<User> userOpt = resolveSessionUser(username);
        if (userOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        User user = userOpt.get();

        if (Boolean.TRUE.equals(user.getPhoneVerified())) {
            message = messageService.getMessage(I18Code.MESSAGE_PHONE_ALREADY_VERIFIED.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        if (!org.springframework.util.StringUtils.hasText(user.getPhoneNumber())) {
            message = messageService.getMessage(I18Code.MESSAGE_PHONE_NUMBER_MISSING_FOR_VERIFICATION.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        try {
            phoneVerificationSupport.generateAndSendOtp(user, OtpType.PHONE_VERIFICATION, username);
        } catch (SmsDeliveryDisabledException ex) {
            message = messageService.getMessage(I18Code.MESSAGE_SMS_DELIVERY_DISABLED.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(
                    503,
                    false,
                    message,
                    null,
                    null,
                    List.of(PhoneVerificationSupport.SMS_DELIVERY_DISABLED_CODE));
        }

        message = messageService.getMessage(I18Code.MESSAGE_PHONE_VERIFICATION_OTP_SENT.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, null, null, null);
    }

    @Override
    @Transactional
    public UserResponse confirmPhoneVerification(String username, String otp, Locale locale) {
        String message;

        Optional<User> userOpt = resolveSessionUser(username);
        if (userOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        User user = userOpt.get();

        boolean valid = phoneVerificationSupport.verifyOtp(user.getId(), otp, OtpType.PHONE_VERIFICATION);
        if (!valid) {
            message = messageService.getMessage(I18Code.MESSAGE_OTP_INVALID_OR_EXPIRED.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        phoneVerificationSupport.markPhoneVerified(user);

        message = messageService.getMessage(I18Code.MESSAGE_PHONE_VERIFIED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        UserDto userDto = toUserDto(userRepository.findById(user.getId()).orElse(user));
        return buildUserResponse(200, true, message, userDto, null, null);
    }

    @Override
    @Transactional
    public UserResponse requestStepUpVerification(String username, Locale locale) {
        String message;

        Optional<User> userOpt = resolveSessionUser(username);
        if (userOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        User user = userOpt.get();

        if (!org.springframework.util.StringUtils.hasText(user.getPhoneNumber())) {
            message = messageService.getMessage(I18Code.MESSAGE_PHONE_NUMBER_MISSING_FOR_VERIFICATION.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        try {
            phoneVerificationSupport.generateAndSendOtp(user, OtpType.STEP_UP, username);
        } catch (SmsDeliveryDisabledException ex) {
            message = messageService.getMessage(I18Code.MESSAGE_SMS_DELIVERY_DISABLED.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(
                    503,
                    false,
                    message,
                    null,
                    null,
                    List.of(PhoneVerificationSupport.SMS_DELIVERY_DISABLED_CODE));
        }

        message = messageService.getMessage(I18Code.MESSAGE_PHONE_VERIFICATION_OTP_SENT.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, null, null, null);
    }

    @Override
    @Transactional
    public UserResponse confirmStepUpVerification(String username, String otp, Locale locale) {
        String message;

        Optional<User> userOpt = resolveSessionUser(username);
        if (userOpt.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        User user = userOpt.get();

        boolean valid = phoneVerificationSupport.verifyOtp(user.getId(), otp, OtpType.STEP_UP);
        if (!valid) {
            message = messageService.getMessage(I18Code.MESSAGE_OTP_INVALID_OR_EXPIRED.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        message = messageService.getMessage(I18Code.MESSAGE_STEP_UP_VERIFIED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, null, null, null);
    }

    // ============================================================
    //  Phone verification — system-facing (called by ldms-authentication)
    // ============================================================

    @Override
    @Transactional
    public UserResponse generateLoginOtp(String usernameOrPhone, Locale locale) {
        String message;

        User user = resolveUserByLoginIdentifier(usernameOrPhone);
        if (user == null) {
            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        if (!org.springframework.util.StringUtils.hasText(user.getPhoneNumber())) {
            message = messageService.getMessage(I18Code.MESSAGE_PHONE_NUMBER_MISSING_FOR_VERIFICATION.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        try {
            phoneVerificationSupport.generateAndSendOtp(user, OtpType.LOGIN_2FA, "system");
        } catch (SmsDeliveryDisabledException ex) {
            message = messageService.getMessage(I18Code.MESSAGE_SMS_DELIVERY_DISABLED.getCode(), new String[]{}, locale);
            return buildUserResponseWithErrors(
                    503,
                    false,
                    message,
                    null,
                    null,
                    List.of(PhoneVerificationSupport.SMS_DELIVERY_DISABLED_CODE));
        }

        message = messageService.getMessage(I18Code.MESSAGE_PHONE_VERIFICATION_OTP_SENT.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, null, null, null);
    }

    @Override
    @Transactional
    public UserResponse verifyLoginOtp(String usernameOrPhone, String otp, Locale locale) {
        String message;

        User user = resolveUserByLoginIdentifier(usernameOrPhone);
        if (user == null) {
            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        boolean valid = phoneVerificationSupport.verifyOtp(user.getId(), otp, OtpType.LOGIN_2FA);
        if (!valid) {
            message = messageService.getMessage(I18Code.MESSAGE_OTP_INVALID_OR_EXPIRED.getCode(), new String[]{}, locale);
            return buildUserResponse(400, false, message, null, null, null);
        }

        message = messageService.getMessage(I18Code.MESSAGE_OTP_VERIFIED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildUserResponse(200, true, message, toUserDto(user), null, null);
    }

    private Optional<User> resolveSessionUser(String sessionPrincipal) {
        if (!StringUtils.hasText(sessionPrincipal)) {
            return Optional.empty();
        }
        String trimmed = sessionPrincipal.trim();
        Optional<User> byUsername = userRepository.findSessionProfileByUsernameIgnoreCaseAndEntityStatusNot(
                trimmed, EntityStatus.DELETED);
        if (byUsername.isPresent()) {
            return byUsername;
        }
        try {
            long id = Long.parseLong(trimmed);
            if (id > 0) {
                return userRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
            }
        } catch (NumberFormatException ignored) {
            // not a numeric JWT principal
        }
        return Optional.empty();
    }

    private User resolveUserByLoginIdentifier(String identifier) {
        if (!org.springframework.util.StringUtils.hasText(identifier)) {
            return null;
        }
        String trimmed = identifier.trim();
        Optional<User> byUsername = userRepository.findSessionProfileByUsernameIgnoreCaseAndEntityStatusNot(
                trimmed, EntityStatus.DELETED);
        if (byUsername.isPresent()) {
            return byUsername.get();
        }
        Optional<User> byPhone = userRepository.findSessionProfileByPhoneNumberAndEntityStatusNot(
                trimmed, EntityStatus.DELETED);
        if (byPhone.isPresent()) {
            return byPhone.get();
        }
        return userRepository.findSessionProfileByEmailIgnoreCaseAndEntityStatusNot(trimmed, EntityStatus.DELETED)
                .orElse(null);
    }
}