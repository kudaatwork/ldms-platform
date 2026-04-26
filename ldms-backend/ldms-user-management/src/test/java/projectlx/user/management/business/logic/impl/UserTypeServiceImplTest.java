package projectlx.user.management.business.logic.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.config.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.auditable.api.UserTypeServiceAuditable;
import projectlx.user.management.business.logic.api.UserTypeService;
import projectlx.user.management.business.validator.api.UserTypeServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserType;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.repository.UserTypeRepository;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.dtos.UserTypeDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.utils.requests.EditUserTypeRequest;
import projectlx.user.management.utils.requests.UserTypeMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserTypeResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserTypeServiceImplTest {

    private UserTypeService userTypeService;
    private UserTypeServiceValidator userTypeServiceValidator;
    private UserTypeRepository userTypeRepository;
    private UserRepository userRepository;
    private UserTypeServiceAuditable userTypeServiceAuditable;
    private MessageService messageService;
    private ModelMapper modelMapper;
    private Configuration configurationMock;

    private final Locale locale = Locale.ENGLISH;
    private final String username = "SYSTEM";

    private CreateUserTypeRequest createUserTypeRequest;
    private EditUserTypeRequest editUserTypeRequest;
    private UserTypeMultipleFiltersRequest userTypeMultipleFiltersRequest;
    private UserType userType;
    private UserTypeDto userTypeDto;
    private List<UserType> userTypeList;
    private List<UserTypeDto> userTypeDtoList;
    private Long userTypeId;
    private Page<UserType> userTypePage;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        userTypeServiceValidator = mock(UserTypeServiceValidator.class);
        userTypeRepository = mock(UserTypeRepository.class);
        userRepository = mock(UserRepository.class);
        userTypeServiceAuditable = mock(UserTypeServiceAuditable.class);
        messageService = mock(MessageService.class);
        modelMapper = mock(ModelMapper.class);
        configurationMock = mock(Configuration.class);

        // Initialize service with mocks
        userTypeService = new UserTypeServiceImpl(
                userTypeServiceValidator,
                messageService,
                modelMapper,
                userTypeRepository,
                userRepository,
                userTypeServiceAuditable
        );

        // Common mock setup for ModelMapper configuration
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);

        // Initialize test data
        userTypeId = 1L;

        userType = new UserType();
        userType.setId(userTypeId);
        userType.setUserTypeName("Administrator");
        userType.setDescription("System Administrator");
        userType.setCreatedAt(LocalDateTime.now());
        userType.setUpdatedAt(LocalDateTime.now());
        userType.setEntityStatus(EntityStatus.ACTIVE);
        userType.setUsers(new ArrayList<>());

        userTypeDto = new UserTypeDto();
        userTypeDto.setId(userTypeId);
        userTypeDto.setUserTypeName("Administrator");
        userTypeDto.setDescription("System Administrator");

        createUserTypeRequest = new CreateUserTypeRequest();
        createUserTypeRequest.setUserTypeName("Administrator");
        createUserTypeRequest.setDescription("System Administrator");

        editUserTypeRequest = new EditUserTypeRequest();
        editUserTypeRequest.setId(userTypeId);
        editUserTypeRequest.setUserTypeName("Super Administrator");
        editUserTypeRequest.setDescription("System Super Administrator");

        userTypeMultipleFiltersRequest = new UserTypeMultipleFiltersRequest();
        userTypeMultipleFiltersRequest.setUserTypeName("Admin");
        userTypeMultipleFiltersRequest.setDescription("System");
        userTypeMultipleFiltersRequest.setSearchValue("Admin");
        userTypeMultipleFiltersRequest.setPage(0);
        userTypeMultipleFiltersRequest.setSize(10);

        userTypeList = new ArrayList<>();
        userTypeList.add(userType);

        userTypeDtoList = new ArrayList<>();
        userTypeDtoList.add(userTypeDto);

        Pageable pageable = PageRequest.of(0, 10);
        userTypePage = new PageImpl<>(userTypeList, pageable, 1);
    }

    @Test
    void create_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(false, List.of("User type name is missing"));
        when(userTypeServiceValidator.isCreateUserTypeRequestValid(any(CreateUserTypeRequest.class), eq(locale))).thenReturn(validatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_CREATE_USER_TYPE_INVALID_REQUEST.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid request");

        // Act
        UserTypeResponse response = userTypeService.create(createUserTypeRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid request", response.getMessage());
        assertNotNull(response.getErrorMessages());
        assertEquals(1, response.getErrorMessages().size());
        assertEquals("User type name is missing", response.getErrorMessages().get(0));
    }

    @Test
    void create_shouldReturnTrueAnd201ForValidRequest() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isCreateUserTypeRequestValid(any(CreateUserTypeRequest.class), eq(locale))).thenReturn(validatorDto);
        when(modelMapper.map(any(CreateUserTypeRequest.class), eq(UserType.class))).thenReturn(userType);
        when(userTypeServiceAuditable.create(any(UserType.class), eq(locale), eq(username))).thenReturn(userType);
        when(modelMapper.map(any(UserType.class), eq(UserTypeDto.class))).thenReturn(userTypeDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_CREATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User type created successfully");

        // Act
        UserTypeResponse response = userTypeService.create(createUserTypeRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User type created successfully", response.getMessage());
        assertNotNull(response.getUserTypeDto());
        assertEquals(userTypeId, response.getUserTypeDto().getId());
    }

    @Test
    void findById_shouldReturnFalseAnd400IfIdIsInvalid() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(false, List.of("User type ID is invalid"));
        when(userTypeServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid ID");

        // Act
        UserTypeResponse response = userTypeService.findById(userTypeId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid ID", response.getMessage());
    }

    @Test
    void findById_shouldReturnFalseAnd404IfUserTypeNotFound() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(userTypeRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User type not found");

        // Act
        UserTypeResponse response = userTypeService.findById(userTypeId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User type not found", response.getMessage());
    }

    @Test
    void findById_shouldReturnTrueAnd200ForValidId() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(userTypeRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userType));
        when(modelMapper.map(any(UserType.class), eq(UserTypeDto.class))).thenReturn(userTypeDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User type retrieved successfully");

        // Act
        UserTypeResponse response = userTypeService.findById(userTypeId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User type retrieved successfully", response.getMessage());
        assertNotNull(response.getUserTypeDto());
        assertEquals(userTypeId, response.getUserTypeDto().getId());
    }

    @Test
    void findAllAsList_shouldReturnFalseAnd404IfNoUserTypeFound() {
        // Arrange
        when(userTypeRepository.findByEntityStatusNot(eq(EntityStatus.DELETED)))
                .thenReturn(Collections.emptyList());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("No user type found");

        // Act
        UserTypeResponse response = userTypeService.findAllAsList(username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("No user type found", response.getMessage());
    }

    @Test
    void findAllAsList_shouldReturnTrueAnd200WithUserTypeList() {
        // Arrange
        when(userTypeRepository.findByEntityStatusNot(eq(EntityStatus.DELETED)))
                .thenReturn(userTypeList);

        // Create a TypeToken for the correct type
        TypeToken<List<UserTypeDto>> typeToken = new TypeToken<List<UserTypeDto>>() {};
        when(modelMapper.map(any(), eq(typeToken.getType()))).thenReturn(userTypeDtoList);

        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User type retrieved successfully");

        // Act
        UserTypeResponse response = userTypeService.findAllAsList(username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User type retrieved successfully", response.getMessage());
        assertNotNull(response.getUserTypeDtoList());
        assertEquals(1, response.getUserTypeDtoList().size());
    }

    @Test
    void update_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(false, List.of("User type update request is invalid"));
        when(userTypeServiceValidator.isRequestValidForEditing(any(EditUserTypeRequest.class), eq(locale))).thenReturn(validatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_TYPE_INVALID_REQUEST.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid update request");

        // Act
        UserTypeResponse response = userTypeService.update(editUserTypeRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid update request", response.getMessage());
    }

    @Test
    void update_shouldReturnFalseAnd400IfUserTypeNotFound() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isRequestValidForEditing(any(EditUserTypeRequest.class), eq(locale))).thenReturn(validatorDto);
        when(userTypeRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User type not found");

        // Act
        UserTypeResponse response = userTypeService.update(editUserTypeRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User type not found", response.getMessage());
    }

    @Test
    void update_shouldReturnTrueAnd201ForValidRequestWithSingleUser() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isRequestValidForEditing(any(EditUserTypeRequest.class), eq(locale))).thenReturn(validatorDto);
        when(userTypeRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userType));
        when(userTypeServiceAuditable.update(any(UserType.class), eq(locale), eq(username))).thenReturn(userType);
        when(modelMapper.map(any(UserType.class), eq(UserTypeDto.class))).thenReturn(userTypeDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_UPDATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User type updated successfully");

        // Set up the userType to have only one user
        User user = new User();
        List<User> users = new ArrayList<>();
        users.add(user);
        userType.setUsers(users);

        // Act
        UserTypeResponse response = userTypeService.update(editUserTypeRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User type updated successfully", response.getMessage());
        assertNotNull(response.getUserTypeDto());
        assertEquals(userTypeId, response.getUserTypeDto().getId());
    }

    @Test
    void update_shouldCreateNewUserTypeWhenMultipleUsersExist() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isRequestValidForEditing(any(EditUserTypeRequest.class), eq(locale))).thenReturn(validatorDto);
        when(userTypeRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userType));
        when(userTypeServiceAuditable.update(any(UserType.class), eq(locale), eq(username))).thenReturn(userType);
        when(modelMapper.map(any(UserType.class), eq(UserTypeDto.class))).thenReturn(userTypeDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_UPDATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User type updated successfully");

        // Set up the userType to have multiple users
        User user1 = new User();
        User user2 = new User();
        List<User> users = new ArrayList<>();
        users.add(user1);
        users.add(user2);
        userType.setUsers(users);

        // Act
        UserTypeResponse response = userTypeService.update(editUserTypeRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User type updated successfully", response.getMessage());
        assertNotNull(response.getUserTypeDto());
        assertEquals(userTypeId, response.getUserTypeDto().getId());
    }

    @Test
    void delete_shouldReturnFalseAnd400IfIdIsInvalid() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(false, List.of("User type ID is invalid"));
        when(userTypeServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid ID");

        // Act
        UserTypeResponse response = userTypeService.delete(userTypeId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid ID", response.getMessage());
    }

    @Test
    void delete_shouldReturnFalseAnd404IfUserTypeNotFound() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(userTypeRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User type not found");

        // Act
        UserTypeResponse response = userTypeService.delete(userTypeId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User type not found", response.getMessage());
    }

    @Test
    void delete_shouldReturnTrueAnd200ForValidRequest() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(userTypeRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userType));
        when(userTypeServiceAuditable.delete(any(UserType.class), eq(locale), anyString())).thenReturn(userType);
        when(modelMapper.map(any(UserType.class), eq(UserTypeDto.class))).thenReturn(userTypeDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_DELETED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User type deleted successfully");

        // Act
        UserTypeResponse response = userTypeService.delete(userTypeId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User type deleted successfully", response.getMessage());
        assertNotNull(response.getUserTypeDto());
        assertEquals(userTypeId, response.getUserTypeDto().getId());
        verify(userTypeServiceAuditable, times(1)).delete(any(UserType.class), eq(locale), anyString());
    }

    @Test
    void findByMultipleFilters_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(false, List.of("Invalid filters request"));
        when(userTypeServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(any(UserTypeMultipleFiltersRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_INVALID_MULTIPLE_FILTERS_REQUEST.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid filters request");

        // Act
        UserTypeResponse response = userTypeService.findByMultipleFilters(userTypeMultipleFiltersRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid filters request", response.getMessage());
    }

    @Test
    void findByMultipleFilters_shouldReturnFalseAnd404IfNoUserTypeFound() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(any(UserTypeMultipleFiltersRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        ValidatorDto stringValidatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isStringValid(anyString(), eq(locale))).thenReturn(stringValidatorDto);
        when(userTypeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("No user type found");

        // Act
        UserTypeResponse response = userTypeService.findByMultipleFilters(userTypeMultipleFiltersRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("No user type found", response.getMessage());
    }

    @Test
    void findByMultipleFilters_shouldReturnTrueAnd200WithUserTypePage() {
        // Arrange
        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(any(UserTypeMultipleFiltersRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        ValidatorDto stringValidatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isStringValid(anyString(), eq(locale))).thenReturn(stringValidatorDto);
        when(userTypeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(userTypePage);
        when(modelMapper.map(any(UserType.class), eq(UserTypeDto.class))).thenReturn(userTypeDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User type retrieved successfully");

        // Act
        UserTypeResponse response = userTypeService.findByMultipleFilters(userTypeMultipleFiltersRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User type retrieved successfully", response.getMessage());
        assertNotNull(response.getUserTypeDtoPage());
        assertEquals(1, response.getUserTypeDtoPage().getTotalElements());
    }

    @Test
    void exportToCsv_shouldReturnByteArray() {
        // Act
        byte[] result = userTypeService.exportToCsv(userTypeDtoList);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);

        // Convert to string to verify content
        String csvContent = new String(result);
        assertTrue(csvContent.contains("ID,USER TYPE NAME,DESCRIPTION"));
        assertTrue(csvContent.contains(userTypeDto.getId().toString()));
        assertTrue(csvContent.contains(userTypeDto.getUserTypeName()));
        assertTrue(csvContent.contains(userTypeDto.getDescription()));
    }

    @Test
    void exportToExcel_shouldReturnByteArray() throws Exception {
        // Act
        byte[] result = userTypeService.exportToExcel(userTypeDtoList);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportToPdf_shouldReturnByteArray() throws Exception {
        // Act
        byte[] result = userTypeService.exportToPdf(userTypeDtoList);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void importUserTypesFromCsv_shouldSuccessfullyImportAllRecords() throws IOException {
        // Arrange
        String csvContent = "USER TYPE NAME,DESCRIPTION\n" +
                "Manager,Department Manager\n" +
                "Supervisor,Team Supervisor";
        InputStream csvInputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        // Mock successful creation for both records
        UserTypeResponse successResponse = new UserTypeResponse();
        successResponse.setSuccess(true);
        successResponse.setStatusCode(201);
        successResponse.setMessage("User type created successfully");
        successResponse.setUserTypeDto(userTypeDto);

        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isCreateUserTypeRequestValid(any(CreateUserTypeRequest.class), any(Locale.class))).thenReturn(validatorDto);
        when(modelMapper.map(any(CreateUserTypeRequest.class), eq(UserType.class))).thenReturn(userType);
        when(userTypeServiceAuditable.create(any(UserType.class), eq(Locale.ENGLISH), eq("IMPORT_SCRIPT"))).thenReturn(userType);
        when(modelMapper.map(any(UserType.class), eq(UserTypeDto.class))).thenReturn(userTypeDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_CREATED_SUCCESSFULLY.getCode()), any(String[].class), eq(Locale.ENGLISH)))
                .thenReturn("User type created successfully");

        // Act
        ImportSummary result = userTypeService.importUserTypesFromCsv(csvInputStream);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
        assertTrue(result.isSuccess());
        assertEquals(2, result.total);
        assertEquals(2, result.success);
        assertEquals(0, result.failed);
        assertTrue(result.getMessage().contains("Import completed successfully"));

        // Verify create was called twice
        verify(userTypeServiceValidator, times(2)).isCreateUserTypeRequestValid(any(CreateUserTypeRequest.class), any(Locale.class));
        verify(userTypeServiceAuditable, times(2)).create(any(UserType.class), eq(Locale.ENGLISH), eq("IMPORT_SCRIPT"));
    }

    @Test
    void importUserTypesFromCsv_shouldHandlePartialSuccess() throws IOException {
        // Arrange
        String csvContent = "USER TYPE NAME,DESCRIPTION\n" +
                "Manager,Department Manager\n" +
                "Invalid,";  // Second record will fail validation
        InputStream csvInputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        // Mock responses
        when(userTypeServiceValidator.isCreateUserTypeRequestValid(any(CreateUserTypeRequest.class), any(Locale.class)))
                .thenAnswer(invocation -> {
                    CreateUserTypeRequest request = invocation.getArgument(0);
                    // First record passes validation, second fails
                    if ("Manager".equals(request.getUserTypeName())) {
                        return createValidatorDto(true);
                    } else {
                        return createValidatorDto(false, List.of("Invalid request"));
                    }
                });

        // For successful record
        when(modelMapper.map(any(CreateUserTypeRequest.class), eq(UserType.class))).thenReturn(userType);
        when(userTypeServiceAuditable.create(any(UserType.class), eq(Locale.ENGLISH), eq("IMPORT_SCRIPT"))).thenReturn(userType);
        when(modelMapper.map(any(UserType.class), eq(UserTypeDto.class))).thenReturn(userTypeDto);

        // Success message
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_TYPE_CREATED_SUCCESSFULLY.getCode()), any(String[].class), eq(Locale.ENGLISH)))
                .thenReturn("User type created successfully");

        // Error message for invalid request
        when(messageService.getMessage(eq(I18Code.MESSAGE_CREATE_USER_TYPE_INVALID_REQUEST.getCode()), any(String[].class), eq(Locale.ENGLISH)))
                .thenReturn("Invalid request");

        // Act
        ImportSummary result = userTypeService.importUserTypesFromCsv(csvInputStream);

        // Assert
        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
        assertTrue(result.isSuccess());
        assertEquals(2, result.total);
        assertEquals(1, result.success);
        assertEquals(1, result.failed);
        assertTrue(result.getMessage().contains("Import completed successfully"));
    }

    @Test
    void importUserTypesFromCsv_shouldHandleExceptions() throws IOException {
        // Arrange
        String csvContent = "USER TYPE NAME,DESCRIPTION\n" +
                "Manager,Department Manager";
        InputStream csvInputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        // Mock an exception during creation
        ValidatorDto validatorDto = createValidatorDto(true);
        when(userTypeServiceValidator.isCreateUserTypeRequestValid(any(CreateUserTypeRequest.class), any(Locale.class))).thenReturn(validatorDto);
        when(modelMapper.map(any(CreateUserTypeRequest.class), eq(UserType.class))).thenThrow(new RuntimeException("Test exception"));

        // Act
        ImportSummary result = userTypeService.importUserTypesFromCsv(csvInputStream);

        // Assert
        assertNotNull(result);
        assertEquals(400, result.getStatusCode());
        assertFalse(result.isSuccess());
        assertEquals(1, result.total);
        assertEquals(0, result.success);
        assertEquals(1, result.failed);
        assertTrue(result.getMessage().contains("Import failed"));
    }

    private ValidatorDto createValidatorDto(boolean success) {
        return createValidatorDto(success, success ? null : List.of("Error message"));
    }

    private ValidatorDto createValidatorDto(boolean success, List<String> errorMessages) {
        return new ValidatorDto(success, null, errorMessages);
    }
}
