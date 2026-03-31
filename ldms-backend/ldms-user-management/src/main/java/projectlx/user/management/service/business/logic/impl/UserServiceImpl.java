package projectlx.user.management.service.business.logic.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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
import projectlx.user.management.service.business.auditable.api.UserAccountServiceAuditable;
import projectlx.user.management.service.business.auditable.api.UserPasswordServiceAuditable;
import projectlx.user.management.service.business.auditable.api.UserPreferencesServiceAuditable;
import projectlx.user.management.service.business.auditable.api.UserSecurityServiceAuditable;
import projectlx.user.management.service.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.service.business.logic.api.UserAccountService;
import projectlx.user.management.service.business.logic.api.UserAddressService;
import projectlx.user.management.service.business.logic.api.UserPasswordService;
import projectlx.user.management.service.business.logic.api.UserPreferencesService;
import projectlx.user.management.service.business.logic.api.UserSecurityService;
import projectlx.user.management.service.business.logic.api.UserService;
import projectlx.user.management.service.business.logic.api.UserTypeService;
import projectlx.user.management.service.business.validator.api.UserServiceValidator;
import projectlx.user.management.service.clients.FileUploadServiceClient;
import projectlx.user.management.service.model.Address;
import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.Gender;
import projectlx.user.management.service.model.User;
import projectlx.user.management.service.model.UserAccount;
import projectlx.user.management.service.model.UserAddressDetails;
import projectlx.user.management.service.model.UserPassword;
import projectlx.user.management.service.model.UserPreferences;
import projectlx.user.management.service.model.UserPreferencesDetails;
import projectlx.user.management.service.model.UserSecurity;
import projectlx.user.management.service.model.UserSecurityDetails;
import projectlx.user.management.service.model.UserType;
import projectlx.user.management.service.model.UserTypeDetails;
import projectlx.user.management.service.model.UserGroup;
import projectlx.user.management.service.repository.UserAccountRepository;
import projectlx.user.management.service.repository.UserAddressRepository;
import projectlx.user.management.service.repository.UserPasswordRepository;
import projectlx.user.management.service.repository.UserPreferencesRepository;
import projectlx.user.management.service.repository.UserRepository;
import projectlx.user.management.service.repository.UserSecurityRepository;
import projectlx.user.management.service.repository.UserTypeRepository;
import projectlx.user.management.service.repository.specification.UserSpecification;
import projectlx.user.management.service.utils.dtos.AddressDto;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.dtos.UserAccountDto;
import projectlx.user.management.service.utils.dtos.UserDto;
import projectlx.user.management.service.utils.dtos.UserGroupDto;
import projectlx.user.management.service.utils.dtos.UserPasswordDto;
import projectlx.user.management.service.utils.dtos.UserPreferencesDto;
import projectlx.user.management.service.utils.dtos.UserRoleDto;
import projectlx.user.management.service.utils.dtos.UserSecurityDto;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.co.zw.shared_library.utils.generators.SecureTokenGenerator;
import projectlx.user.management.service.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.service.utils.requests.CreateAddressRequest;
import projectlx.user.management.service.utils.requests.CreateUserPasswordRequest;
import projectlx.user.management.service.utils.requests.CreateUserPreferencesRequest;
import projectlx.user.management.service.utils.requests.CreateUserRequest;
import projectlx.user.management.service.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.service.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.service.utils.requests.EditUserRequest;
import projectlx.user.management.service.utils.requests.ForgotPasswordRequest;
import projectlx.user.management.service.utils.requests.NotificationRequest;
import projectlx.user.management.service.utils.requests.UsersMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserAccountResponse;
import projectlx.user.management.service.utils.responses.AddressResponse;
import projectlx.user.management.service.utils.responses.UserPreferencesResponse;
import projectlx.user.management.service.utils.responses.UserResponse;
import projectlx.user.management.service.utils.responses.UserSecurityResponse;
import projectlx.user.management.service.utils.responses.UserTypeResponse;
import projectlx.user.management.service.utils.excel.UserExcelListener;
import projectlx.user.management.service.utils.excel.UserExcelModel;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

        UserType userType = new UserType();

        if (createUserRequest.getUserTypeDetails() != null) {

            Optional<UserType> userTypeOptional = userTypeRepository.findByUserTypeNameAndEntityStatusNot(
                    createUserRequest.getUserTypeDetails().getUserTypeName(), EntityStatus.DELETED);

            if (userTypeOptional.isPresent()) {
                userType = userTypeOptional.get();
            } else {

                CreateUserTypeRequest createUserTypeRequest = createUserTypeRequest(createUserRequest.getUserTypeDetails());

                UserTypeResponse userTypeResponse = userTypeService.create(createUserTypeRequest, locale, username);

                userTypeRepository.findByIdAndEntityStatusNot(
                                userTypeResponse.getUserTypeDto().getId(), EntityStatus.DELETED)
                        .ifPresent(retrievedType -> userToBeSaved.setUserType(retrievedType));
            }

            userToBeSaved.setUserType(userType);
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
                addressDtoForResponse = modelMapper.map(address, AddressDto.class);
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

            userPreferencesRepository.findByIdAndEntityStatusNot(
                            userPreferencesResponse.getUserPreferencesDto().getId(), EntityStatus.DELETED)
                    .ifPresent(userSaved::setUserPreferences);
        }

        UserSecurityResponse userSecurityResponse = new UserSecurityResponse();

        if (createUserRequest.getUserSecurityDetails() != null) {

            CreateUserSecurityRequest createUserSecurityRequest = createUserSecurityRequest(
                    createUserRequest.getUserSecurityDetails(), userSaved.getId());

            userSecurityResponse = userSecurityService.create(createUserSecurityRequest, locale, username);

            userSecurityRepository.findByIdAndEntityStatusNot(
                            userSecurityResponse.getUserSecurityDto().getId(), EntityStatus.DELETED)
                    .ifPresent(userSaved::setUserSecurity);
        }

        if (createUserRequest.getNationalIdUploadId() != null) {
            userSaved.setNationalIdUploadId(createUserRequest.getNationalIdUploadId());
        }
        if (createUserRequest.getPassportUploadId() != null) {
            userSaved.setPassportUploadId(createUserRequest.getPassportUploadId());
        } else if (createUserRequest.getNationalIdUpload() != null || createUserRequest.getPassportUpload() != null) {

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

                if (fileUploadResponse.isSuccess()) {

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
                }
            } catch (Exception e) {
                logger.error("Error while uploading the file for user creation: ", e);
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
    public UserResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userServiceValidator.isIdValid(id, locale);

        if(validatorDto == null || !validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
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

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserDto userDto = modelMapper.map(user, UserDto.class);

        // Retrieve user account linked to the user
        UserAccount userAccount = user.getUserAccount();

        // Map user account to DTO
        UserAccountDto userAccountDto = modelMapper.map(userAccount, UserAccountDto.class);

        // Set user account in the UserDto
        userDto.setUserAccountDto(userAccountDto);

        // Retrieve user address linked to the user
        Address address = user.getAddress();

        // Map user address to DTO
        AddressDto addressDto = modelMapper.map(address, AddressDto.class);

        // Set user address in the UserDto
        userDto.setAddressDto(addressDto);

        // Retrieve user group linked to the user
        UserGroup userGroup = user.getUserGroup();

        // Map user group to DTO
        UserGroupDto userGroupDto = modelMapper.map(userGroup, UserGroupDto.class);

        // Set user group in the UserDto
        userDto.setUserGroupDto(userGroupDto);

        // Retrieve user password linked to the user
        UserPassword userPassword = user.getUserPassword();

        // Map user password to DTO
        UserPasswordDto userPasswordDto = modelMapper.map(userPassword, UserPasswordDto.class);

        // Set user password in the UserDto
        userDto.setUserPasswordDto(userPasswordDto);

        // Retrieve user preferences linked to the user
        UserPreferences userPreferences = user.getUserPreferences();

        // Map user preferences to DTO
        UserPreferencesDto userPreferencesDto = modelMapper.map(userPreferences, UserPreferencesDto.class);

        // Set user preferences in the UserDto
        userDto.setUserPreferencesDto(userPreferencesDto);

        // Retrieve user security linked to the user
        UserSecurity userSecurity = user.getUserSecurity();

        // Map user security to DTO
        UserSecurityDto userSecurityDto = modelMapper.map(userSecurity, UserSecurityDto.class);

        // Set user security in the UserDto
        userDto.setUserSecurityDto(userSecurityDto);

        message = messageService.getMessage(I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserResponse(200, true, message, userDto, null,
                null);
    }

    @Override
    public UserResponse findByUsername(String username, Locale locale) {

        String message = "";

        boolean isIdValid = userServiceValidator.isStringValid(username);

        if(!isIdValid) {

            message = messageService.getMessage(I18Code.MESSAGE_USERNAME_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserResponse(400, false, message,null, null,
                    null);
        }

        Optional<User> userRetrieved = userRepository.findByUsernameAndEntityStatusNot(username, EntityStatus.DELETED);

        if (userRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserResponse(404, false, message,null, null,
                    null);
        }

        User userReturned = userRetrieved.get();

        // Always map mandatory DTOs
        UserAccountDto userAccountDto = modelMapper.map(userReturned.getUserAccount(), UserAccountDto.class);
        UserPasswordDto userPasswordDto = modelMapper.map(userReturned.getUserPassword(), UserPasswordDto.class);

        // Map main user
        UserDto userDto = modelMapper.map(userReturned, UserDto.class);
        userDto.setUserAccountDto(userAccountDto);
        userDto.setUserPasswordDto(userPasswordDto);

        // Optionally map and set user security
        if (userReturned.getUserSecurity() != null) {
            UserSecurityDto userSecurityDto = modelMapper.map(userReturned.getUserSecurity(), UserSecurityDto.class);
            userDto.setUserSecurityDto(userSecurityDto);
        }

        // Optionally map and set user group and roles
        if (userReturned.getUserGroup() != null) {
            UserGroupDto userGroupDto = modelMapper.map(userReturned.getUserGroup(), UserGroupDto.class);

            if (userReturned.getUserGroup().getUserRoles() != null) {
                List<UserRoleDto> userRoleDtoList = modelMapper.map(
                        userReturned.getUserGroup().getUserRoles(),
                        new TypeToken<List<UserRoleDto>>(){}.getType()
                );
                userGroupDto.setUserRoleDtoSet(userRoleDtoList);
            }

            userDto.setUserGroupDto(userGroupDto);
        }

        // Optionally map and set address
        if (userReturned.getAddress() != null) {
            AddressDto addressDto = modelMapper.map(userReturned.getAddress(), AddressDto.class);
            userDto.setAddressDto(addressDto);
        }

        // Optionally map and set preferences
        if (userReturned.getUserPreferences() != null) {
            UserPreferencesDto userPreferencesDto = modelMapper.map(userReturned.getUserPreferences(), UserPreferencesDto.class);
            userDto.setUserPreferencesDto(userPreferencesDto);
        }

        message = messageService.getMessage(I18Code.MESSAGE_USER_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserResponse(200, true, message, userDto, null,
                null);
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
    public UserResponse update(EditUserRequest editUserRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = userServiceValidator.isRequestValidForEditing(editUserRequest, locale);

        if(!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
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
        userToBeEdited.setUsername(editUserRequest.getUsername());
        userToBeEdited.setEmail(editUserRequest.getEmail());
        userToBeEdited.setFirstName(editUserRequest.getFirstName());
        userToBeEdited.setLastName(editUserRequest.getLastName());
        userToBeEdited.setGender(Gender.valueOf(editUserRequest.getGender()));
        userToBeEdited.setNationalIdNumber(editUserRequest.getNationalIdNumber());
        userToBeEdited.setPassportNumber(editUserRequest.getPassportNumber());
        userToBeEdited.setPhoneNumber(editUserRequest.getPhoneNumber());
        userToBeEdited.setDateOfBirth(Date.valueOf(editUserRequest.getDateOfBirth()));

        User userEdited = userServiceAuditable.update(userToBeEdited, locale, username);

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

            logger.error("Error while uploading the file for user creation: ", e);
        }

        User userEditedForTheSecondTime = userServiceAuditable.update(userEdited, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserDto userDtoReturned = modelMapper.map(userEditedForTheSecondTime, UserDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserResponse(201, true, message, userDtoReturned, null,
                null);
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

        Optional<UserPreferences> userPreferencesRetrieved = userPreferencesRepository.findByIdAndEntityStatusNot(
                userToBeDeleted.getUserPreferences().getId(), EntityStatus.DELETED);

        if (userPreferencesRetrieved.isPresent()) {
            UserPreferences userPreferencesDeleted = userPreferencesServiceAuditable.delete(userToBeDeleted.getUserPreferences(),
                    locale);
        }

        Optional<UserSecurity> userSecurityReceived = userSecurityRepository.findByIdAndEntityStatusNot(
                userToBeDeleted.getUserSecurity().getId(), EntityStatus.DELETED);

        if (userSecurityReceived.isPresent()) {
            UserSecurity userSecurityDeleted = userSecurityServiceAuditable.delete(userToBeDeleted.getUserSecurity(),
                    locale);
        }

        User userDeleted = userServiceAuditable.delete(userToBeDeleted, locale);

        UserDto userDtoReturned = modelMapper.map(userDeleted, UserDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserResponse(200, true, message, userDtoReturned, null,
                null);
    }

    @Override
    public UserResponse findByMultipleFilters(UsersMultipleFiltersRequest usersMultipleFiltersRequest,
                                              String username, Locale locale) {

        String message = "";

        Specification<User> spec = null;
        spec = addToSpec(spec, UserSpecification::deleted);

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

        long totalCount = userRepository.count(spec);
        int maxPage = (int) Math.ceil((double) totalCount / usersMultipleFiltersRequest.getSize());

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

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("USER EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (UserDto user : users) {
            table.addCell(String.valueOf(user.getId()));
            table.addCell(safe(user.getUsername()));
            table.addCell(safe(user.getEmail()));
            table.addCell(safe(user.getFirstName()));
            table.addCell(safe(user.getLastName()));
            table.addCell(enumToString(user.getGender()));
            table.addCell(safe(user.getPhoneNumber()));
            table.addCell(safe(user.getNationalIdNumber()));
            table.addCell(safe(user.getPassportNumber()));
            table.addCell(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
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

            return buildUserResponse(200, true, message, null, null, null);
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

        return buildUserResponse(200, true, message, null, null, null);
    }

    @Override
    public UserResponse forgotPassword(ForgotPasswordRequest forgotPasswordRequest, Locale locale) {

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

        // Send password reset email
        sendPasswordResetEmail(user, resetToken);           // Email
        sendPasswordResetSms(user, resetToken);             // SMS (new)
        sendPasswordResetInAppNotification(user);           // In-App (new)
        sendPasswordResetWhatsApp(user, resetToken);     // WhatsApp (optional)

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
    public UserResponse findByPhoneNumberOrEmail(String phoneNumberOrEmail, Locale locale) {

        String message = "";

        boolean isPhoneNumberOrEmailValid = userServiceValidator.isStringValid(phoneNumberOrEmail);

        if(!isPhoneNumberOrEmailValid) {

            message = messageService.getMessage(I18Code.MESSAGE_PHONE_NUMBER_OR_EMAIL_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
        }

        // Try to find by phone number first
        Optional<User> userRetrieved = userRepository.findByPhoneNumberAndEntityStatusNot(phoneNumberOrEmail, EntityStatus.DELETED);

        // If not found by phone number, try to find by email
        if (userRetrieved.isEmpty()) {
            userRetrieved = userRepository.findByEmailAndEntityStatusNot(phoneNumberOrEmail, EntityStatus.DELETED);
        }

        if (userRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserResponse(404, false, message, null, null,
                    null);
        }

        User userReturned = userRetrieved.get();

        // Always map mandatory DTOs
        UserAccountDto userAccountDto = modelMapper.map(userReturned.getUserAccount(), UserAccountDto.class);
        UserPasswordDto userPasswordDto = modelMapper.map(userReturned.getUserPassword(), UserPasswordDto.class);

        // Map main user
        UserDto userDto = modelMapper.map(userReturned, UserDto.class);
        userDto.setUserAccountDto(userAccountDto);
        userDto.setUserPasswordDto(userPasswordDto);

        // Optionally map and set user security
        if (userReturned.getUserSecurity() != null) {
            UserSecurityDto userSecurityDto = modelMapper.map(userReturned.getUserSecurity(), UserSecurityDto.class);
            userDto.setUserSecurityDto(userSecurityDto);
        }

        // Optionally map and set user group and roles
        if (userReturned.getUserGroup() != null) {
            UserGroupDto userGroupDto = modelMapper.map(userReturned.getUserGroup(), UserGroupDto.class);

            if (userReturned.getUserGroup().getUserRoles() != null) {
                List<UserRoleDto> userRoleDtoList = modelMapper.map(
                        userReturned.getUserGroup().getUserRoles(),
                        new TypeToken<List<UserRoleDto>>(){}.getType()
                );
                userGroupDto.setUserRoleDtoSet(userRoleDtoList);
            }

            userDto.setUserGroupDto(userGroupDto);
        }

        // Optionally map and set address
        if (userReturned.getAddress() != null) {
            AddressDto addressDto = modelMapper.map(userReturned.getAddress(), AddressDto.class);
            userDto.setAddressDto(addressDto);
        }

        // Optionally map and set preferences
        if (userReturned.getUserPreferences() != null) {
            UserPreferencesDto userPreferencesDto = modelMapper.map(userReturned.getUserPreferences(), UserPreferencesDto.class);
            userDto.setUserPreferencesDto(userPreferencesDto);
        }

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
    public UserResponse findByOrganizationId(Long organizationId, Locale locale, String username) {

        String message = "";

        boolean isIdValid = userServiceValidator.isIdValid(organizationId);

        if(!isIdValid) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
        }

        List<User> userList = userRepository.findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED);

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
    public UserResponse findByAgentId(Long agentId, Locale locale, String username) {

        String message = "";

        boolean isIdValid = userServiceValidator.isIdValid(agentId);

        if(!isIdValid) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserResponse(400, false, message, null, null,
                    null);
        }

        List<User> userList = userRepository.findByAgentIdAndEntityStatusNot(agentId, EntityStatus.DELETED);

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
            UserDto userDto = modelMapper.map(user, UserDto.class);
            userDtoList.add(userDto);
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
        Map<String, Object> data = Map.of("userName", newUser.getFirstName());

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

        return request;
    }

    // This method would be called right after newUser is saved
    public void sendWhatsAppVerificationReminder(User newUser) {
        // 1. Prepare the dynamic data for the template
        Map<String, Object> data = Map.of(
                "userName", newUser.getFirstName(),
                "userEmail", newUser.getEmail()
        );

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

        // 1. Construct the full verification link for your frontend application
        String verificationLink = "http://localhost:9012/api/v1/system/user/verify-email?token=" + verificationToken +
                "&email=" + newUser.getEmail();

        // 2. Prepare the dynamic data that will replace the placeholders in the template
        Map<String, Object> data = Map.of(
                "userName", newUser.getUsername(),
                "verificationLink", verificationLink,
                "email", newUser.getEmail()
        );

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

        // 5. Send IN-APP notification for registration
        NotificationRequest inAppNotificationRequest = new NotificationRequest(
                UUID.randomUUID().toString(),
                "USER_REGISTRATION_IN_APP", // In-App template
                recipient,
                data,
                null
        );

        // 6. Publish both notifications to RabbitMQ
        try {
            logger.info("Publishing verification email request for user: {}", newUser.getEmail());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", emailNotificationRequest);
            logger.info("Successfully published verification email request for user: {}", newUser.getEmail());

            logger.info("Publishing verification in-app notification for user: {}", newUser.getId());
            rabbitTemplate.convertAndSend("notifications.direct", "notifications.send", inAppNotificationRequest);
            logger.info("Successfully published verification in-app notification for user: {}", newUser.getId());
        } catch (Exception e) {
            logger.error("Failed to publish verification notifications for user: {}. Error: {}",
                    newUser.getEmail(), e.getMessage());
        }
    }

    public void sendPasswordResetEmail(User user, String resetToken) {
        // Construct the reset link for your frontend
        String resetLink = "https://localhost:9012/reset-password?token=" + resetToken + "&email=" + user.getEmail();

        // Prepare the dynamic data
        Map<String, Object> data = Map.of(
                "firstName", user.getFirstName(),
                "userName", user.getUsername(),
                "Email", user.getEmail(),
                "resetLink", resetLink
        );

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
    public void sendPasswordResetSms(User user, String resetToken) {
        // Prepare the dynamic data
        Map<String, Object> data = Map.of(
                "userName", user.getUsername(),
                "firstName", user.getFirstName()
        );

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
    public void sendPasswordResetWhatsApp(User user, String resetToken) {
        // Prepare the dynamic data
        Map<String, Object> data = Map.of(
                "userName", user.getUsername(),
                "firstName", user.getFirstName(),
                "email", user.getEmail()
        );

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
        Map<String, Object> data = Map.of(
                "Email", user.getEmail(),
                "userName", user.getUsername(),
                "firstName", user.getFirstName()
        );

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

    private CreateUserSecurityRequest createUserSecurityRequest(UserSecurityDetails userSecurityDetails,
                                                                Long userId) {

        CreateUserSecurityRequest createUserSecurityRequest = new CreateUserSecurityRequest();

        createUserSecurityRequest.setSecurityQuestion_1(userSecurityDetails.getSecurityQuestion_1());
        createUserSecurityRequest.setSecurityQuestion_2(userSecurityDetails.getSecurityQuestion_2());
        createUserSecurityRequest.setSecurityAnswer_1(userSecurityDetails.getSecurityAnswer_1());
        createUserSecurityRequest.setSecurityAnswer_2(userSecurityDetails.getSecurityAnswer_2());
        createUserSecurityRequest.setTwoFactorAuthSecret(userSecurityDetails.getTwoFactorAuthSecret());
        createUserSecurityRequest.setIsTwoFactorEnabled(userSecurityDetails.getIsTwoFactorEnabled());
        createUserSecurityRequest.setUserId(userId);

        return createUserSecurityRequest;
    }

    private CreateUserTypeRequest createUserTypeRequest(UserTypeDetails userTypeDetails) {

        CreateUserTypeRequest createUserTypeRequest = new CreateUserTypeRequest();

        createUserTypeRequest.setUserTypeName(userTypeDetails.getUserTypeName());
        createUserTypeRequest.setDescription(userTypeDetails.getDescription());

        return createUserTypeRequest;
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
                nationalIdFile.setExpiresAt(LocalDateTime.parse(createUserRequest.getNationalIdExpiryDate()));
            }

            filesMetadata.add(nationalIdFile);
        }

        if (hasPassportUpload) {

            SingleFileUploadRequest passportFile = new SingleFileUploadRequest();
            passportFile.setFile(createUserRequest.getPassportUpload());
            passportFile.setFileType(String.valueOf(FileType.PASSPORT));

            if (createUserRequest.getPassportExpiryDate() != null) {
                passportFile.setExpiresAt(LocalDateTime.parse(createUserRequest.getNationalIdExpiryDate()));
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
                nationalIdFile.setExpiresAt(LocalDateTime.parse(editUserRequest.getNationalIdExpiryDate()));
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
                passportFile.setExpiresAt(LocalDateTime.parse(editUserRequest.getPassportExpiryDate()));
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
}