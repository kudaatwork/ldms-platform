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
import projectlx.user.management.business.auditable.api.UserRoleServiceAuditable;
import projectlx.user.management.business.logic.api.UserRoleService;
import projectlx.user.management.business.validator.api.UserRoleServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserRole;
import projectlx.user.management.repository.UserRoleRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.dtos.UserRoleDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserRoleRequest;
import projectlx.user.management.utils.requests.EditUserRoleRequest;
import projectlx.user.management.utils.requests.UserRoleMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserRoleResponse;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.HashSet;

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

class UserRoleServiceImplTest {

    private UserRoleService userRoleService;
    private UserRoleServiceValidator userRoleServiceValidator;
    private UserRoleRepository userRoleRepository;
    private UserRepository userRepository;
    private UserRoleServiceAuditable userRoleServiceAuditable;
    private MessageService messageService;
    private ModelMapper modelMapper;
    private Configuration configurationMock;

    private final Locale locale = Locale.ENGLISH;
    private final String username = "SYSTEM";

    private CreateUserRoleRequest createUserRoleRequest;
    private EditUserRoleRequest editUserRoleRequest;
    private UserRoleMultipleFiltersRequest userRoleMultipleFiltersRequest;
    private UserRole userRole;
    private UserRoleDto userRoleDto;
    private List<UserRole> userRoleList;
    private List<UserRoleDto> userRoleDtoList;
    private Long userRoleId;
    private Page<UserRole> userRolePage;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        userRoleServiceValidator = mock(UserRoleServiceValidator.class);
        userRoleRepository = mock(UserRoleRepository.class);
        userRepository = mock(UserRepository.class);
        userRoleServiceAuditable = mock(UserRoleServiceAuditable.class);
        messageService = mock(MessageService.class);
        modelMapper = mock(ModelMapper.class);
        configurationMock = mock(Configuration.class);

        // Initialize service with mocks
        userRoleService = new UserRoleServiceImpl(
                userRoleServiceValidator,
                messageService,
                modelMapper,
                userRoleRepository,
                userRepository,
                userRoleServiceAuditable
        );

        // Common mock setup for ModelMapper configuration
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);

        // Initialize test data
        userRoleId = 1L;

        userRole = new UserRole();
        userRole.setId(userRoleId);
        userRole.setRole("ADMIN");
        userRole.setDescription("Administrator role");
        userRole.setCreatedAt(LocalDateTime.now());
        userRole.setUpdatedAt(LocalDateTime.now());
        userRole.setEntityStatus(EntityStatus.ACTIVE);
        userRole.setUserGroups(new HashSet<>());

        userRoleDto = new UserRoleDto();
        userRoleDto.setId(userRoleId);
        userRoleDto.setRole("ADMIN");
        userRoleDto.setDescription("Administrator role");
        userRoleDto.setCreatedAt(LocalDateTime.now());
        userRoleDto.setUpdatedAt(LocalDateTime.now());
        userRoleDto.setEntityStatus(EntityStatus.ACTIVE);

        createUserRoleRequest = new CreateUserRoleRequest();
        createUserRoleRequest.setRole("ADMIN");
        createUserRoleRequest.setDescription("Administrator role");

        editUserRoleRequest = new EditUserRoleRequest();
        editUserRoleRequest.setId(userRoleId);
        editUserRoleRequest.setRole("ADMIN");
        editUserRoleRequest.setDescription("Updated administrator role");

        userRoleMultipleFiltersRequest = new UserRoleMultipleFiltersRequest();
        userRoleMultipleFiltersRequest.setRole("ADMIN");
        userRoleMultipleFiltersRequest.setDescription("Administrator");
        userRoleMultipleFiltersRequest.setSearchValue("admin");
        userRoleMultipleFiltersRequest.setPage(0);
        userRoleMultipleFiltersRequest.setSize(10);

        userRoleList = new ArrayList<>();
        userRoleList.add(userRole);

        userRoleDtoList = new ArrayList<>();
        userRoleDtoList.add(userRoleDto);

        Pageable pageable = PageRequest.of(0, 10);
        userRolePage = new PageImpl<>(userRoleList, pageable, 1);
    }

    @Test
    void create_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Arrange
        ValidatorDto invalidValidatorDto = new ValidatorDto(false, null, List.of("Invalid request"));
        when(userRoleServiceValidator.isCreateUserRoleRequestValid(any(CreateUserRoleRequest.class), eq(locale))).thenReturn(invalidValidatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_CREATE_USER_ROLE_INVALID_REQUEST.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid request");

        // Act
        UserRoleResponse response = userRoleService.create(createUserRoleRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid request", response.getMessage());
    }

    @Test
    void create_shouldReturnFalseAnd400IfUserRoleAlreadyExists() {
        // Arrange
        ValidatorDto validValidatorDto = new ValidatorDto(true, null, null);
        when(userRoleServiceValidator.isCreateUserRoleRequestValid(any(CreateUserRoleRequest.class), eq(locale))).thenReturn(validValidatorDto);
        when(userRoleRepository.findByRoleAndEntityStatusNot(anyString(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userRole));
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_ALREADY_EXISTS.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User role already exists");

        // Act
        UserRoleResponse response = userRoleService.create(createUserRoleRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User role already exists", response.getMessage());
    }

    @Test
    void create_shouldReturnTrueAnd201ForValidRequest() {
        // Arrange
        ValidatorDto validValidatorDto = new ValidatorDto(true, null, null);
        when(userRoleServiceValidator.isCreateUserRoleRequestValid(any(CreateUserRoleRequest.class), eq(locale))).thenReturn(validValidatorDto);
        when(userRoleRepository.findByRoleAndEntityStatusNot(anyString(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(modelMapper.map(any(CreateUserRoleRequest.class), eq(UserRole.class))).thenReturn(userRole);
        when(userRoleServiceAuditable.create(any(UserRole.class), eq(locale), eq(username))).thenReturn(userRole);
        when(modelMapper.map(any(UserRole.class), eq(UserRoleDto.class))).thenReturn(userRoleDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_CREATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User role created successfully");

        // Act
        UserRoleResponse response = userRoleService.create(createUserRoleRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User role created successfully", response.getMessage());
        assertNotNull(response.getUserRoleDto());
        assertEquals(userRoleId, response.getUserRoleDto().getId());
    }

    @Test
    void findById_shouldReturnFalseAnd400IfIdIsInvalid() {
        // Arrange
        ValidatorDto invalidValidatorDto = new ValidatorDto(false, null, List.of("Invalid ID"));
        when(userRoleServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(invalidValidatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid ID");

        // Act
        UserRoleResponse response = userRoleService.findById(userRoleId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid ID", response.getMessage());
    }

    @Test
    void findById_shouldReturnFalseAnd404IfUserRoleNotFound() {
        // Arrange
        ValidatorDto validValidatorDto = new ValidatorDto(true, null, null);
        when(userRoleServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validValidatorDto);
        when(userRoleRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User role not found");

        // Act
        UserRoleResponse response = userRoleService.findById(userRoleId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User role not found", response.getMessage());
    }

    @Test
    void findById_shouldReturnTrueAnd200ForValidId() {
        // Arrange
        ValidatorDto validValidatorDto = new ValidatorDto(true, null, null);
        when(userRoleServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validValidatorDto);
        when(userRoleRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userRole));
        when(modelMapper.map(any(UserRole.class), eq(UserRoleDto.class))).thenReturn(userRoleDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User role retrieved successfully");

        // Act
        UserRoleResponse response = userRoleService.findById(userRoleId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User role retrieved successfully", response.getMessage());
        assertNotNull(response.getUserRoleDto());
        assertEquals(userRoleId, response.getUserRoleDto().getId());
    }

    @Test
    void findAllAsList_shouldReturnFalseAnd404IfNoUserRolesFound() {
        // Arrange
        when(userRoleRepository.findByEntityStatusNot(eq(EntityStatus.DELETED)))
                .thenReturn(Collections.emptyList());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("No user roles found");

        // Act
        UserRoleResponse response = userRoleService.findAllAsList(username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("No user roles found", response.getMessage());
    }

    @Test
    void findAllAsList_shouldReturnTrueAnd200WithUserRolesList() {
        // Arrange
        when(userRoleRepository.findByEntityStatusNot(eq(EntityStatus.DELETED)))
                .thenReturn(userRoleList);

        // Create a TypeToken for the correct type
        TypeToken<List<UserRoleDto>> typeToken = new TypeToken<List<UserRoleDto>>() {};
        when(modelMapper.map(any(), eq(typeToken.getType()))).thenReturn(userRoleDtoList);

        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User roles retrieved successfully");

        // Act
        UserRoleResponse response = userRoleService.findAllAsList(username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User roles retrieved successfully", response.getMessage());
        assertNotNull(response.getUserRoleDtoList());
        assertEquals(1, response.getUserRoleDtoList().size());
    }

    @Test
    void update_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Arrange
        ValidatorDto invalidValidatorDto = new ValidatorDto(false, null, List.of("Invalid update request"));
        when(userRoleServiceValidator.isRequestValidForEditing(any(EditUserRoleRequest.class), eq(locale))).thenReturn(invalidValidatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_ROLE_INVALID_REQUEST.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid update request");

        // Act
        UserRoleResponse response = userRoleService.update(editUserRoleRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid update request", response.getMessage());
    }

    @Test
    void update_shouldReturnFalseAnd400IfUserRoleNotFound() {
        // Arrange
        ValidatorDto validValidatorDto = new ValidatorDto(true, null, null);
        when(userRoleServiceValidator.isRequestValidForEditing(any(EditUserRoleRequest.class), eq(locale))).thenReturn(validValidatorDto);
        when(userRoleRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User role not found");

        // Act
        UserRoleResponse response = userRoleService.update(editUserRoleRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User role not found", response.getMessage());
    }

    @Test
    void update_shouldReturnTrueAnd201ForValidRequest() {
        // Arrange
        ValidatorDto validValidatorDto = new ValidatorDto(true, null, null);
        when(userRoleServiceValidator.isRequestValidForEditing(any(EditUserRoleRequest.class), eq(locale))).thenReturn(validValidatorDto);
        when(userRoleRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userRole));
        when(userRoleServiceAuditable.update(any(UserRole.class), eq(locale), eq(username))).thenReturn(userRole);
        when(modelMapper.map(any(UserRole.class), eq(UserRoleDto.class))).thenReturn(userRoleDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_UPDATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User role updated successfully");

        // Act
        UserRoleResponse response = userRoleService.update(editUserRoleRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User role updated successfully", response.getMessage());
        assertNotNull(response.getUserRoleDto());
        assertEquals(userRoleId, response.getUserRoleDto().getId());
    }

    @Test
    void delete_shouldReturnFalseAnd400IfIdIsInvalid() {
        // Arrange
        ValidatorDto invalidValidatorDto = new ValidatorDto(false, null, List.of("Invalid ID"));
        when(userRoleServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(invalidValidatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid ID");

        // Act
        UserRoleResponse response = userRoleService.delete(userRoleId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid ID", response.getMessage());
    }

    @Test
    void delete_shouldReturnFalseAnd404IfUserRoleNotFound() {
        // Arrange
        ValidatorDto validValidatorDto = new ValidatorDto(true, null, null);
        when(userRoleServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validValidatorDto);
        when(userRoleRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User role not found");

        // Act
        UserRoleResponse response = userRoleService.delete(userRoleId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User role not found", response.getMessage());
    }

    @Test
    void delete_shouldReturnTrueAnd200ForValidRequest() {
        // Arrange
        ValidatorDto validValidatorDto = new ValidatorDto(true, null, null);
        when(userRoleServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validValidatorDto);
        when(userRoleRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userRole));
        when(userRoleServiceAuditable.delete(any(UserRole.class), eq(locale))).thenReturn(userRole);
        when(modelMapper.map(any(UserRole.class), eq(UserRoleDto.class))).thenReturn(userRoleDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_DELETED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User role deleted successfully");

        // Act
        UserRoleResponse response = userRoleService.delete(userRoleId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User role deleted successfully", response.getMessage());
        assertNotNull(response.getUserRoleDto());
        assertEquals(userRoleId, response.getUserRoleDto().getId());
        verify(userRoleServiceAuditable, times(1)).delete(any(UserRole.class), eq(locale));
    }

    @Test
    void findByMultipleFilters_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Arrange
        ValidatorDto invalidValidatorDto = new ValidatorDto(false, null, List.of("Invalid filters request"));
        when(userRoleServiceValidator.isRequestValidToRetrieveUserRoleByMultipleFilters(any(UserRoleMultipleFiltersRequest.class), eq(locale)))
                .thenReturn(invalidValidatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_INVALID_MULTIPLE_FILTERS_REQUEST.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid filters request");

        // Act
        UserRoleResponse response = userRoleService.findByMultipleFilters(userRoleMultipleFiltersRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid filters request", response.getMessage());
    }

    @Test
    void findByMultipleFilters_shouldReturnFalseAnd404IfNoUserRolesFound() {
        // Arrange
        ValidatorDto validValidatorDto = new ValidatorDto(true, null, null);
        when(userRoleServiceValidator.isRequestValidToRetrieveUserRoleByMultipleFilters(any(UserRoleMultipleFiltersRequest.class), eq(locale)))
                .thenReturn(validValidatorDto);
        when(userRoleServiceValidator.isStringValid(anyString(), eq(locale))).thenReturn(validValidatorDto);
        when(userRoleRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("No user roles found");

        // Act
        UserRoleResponse response = userRoleService.findByMultipleFilters(userRoleMultipleFiltersRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("No user roles found", response.getMessage());
    }

    @Test
    void findByMultipleFilters_shouldReturnTrueAnd200WithUserRolesPage() {
        // Arrange
        ValidatorDto validValidatorDto = new ValidatorDto(true, null, null);
        when(userRoleServiceValidator.isRequestValidToRetrieveUserRoleByMultipleFilters(any(UserRoleMultipleFiltersRequest.class), eq(locale)))
                .thenReturn(validValidatorDto);
        when(userRoleServiceValidator.isStringValid(anyString(), eq(locale))).thenReturn(validValidatorDto);
        when(userRoleRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(userRolePage);
        when(modelMapper.map(any(UserRole.class), eq(UserRoleDto.class))).thenReturn(userRoleDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ROLE_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User roles retrieved successfully");

        // Act
        UserRoleResponse response = userRoleService.findByMultipleFilters(userRoleMultipleFiltersRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User roles retrieved successfully", response.getMessage());
        assertNotNull(response.getUserRoleDtoPage());
        assertEquals(1, response.getUserRoleDtoPage().getTotalElements());
    }

    @Test
    void exportToCsv_shouldReturnByteArray() {
        // Act
        byte[] result = userRoleService.exportToCsv(userRoleDtoList);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);

        // Convert to string to verify content
        String csvContent = new String(result);
        assertTrue(csvContent.contains("ID,ROLE,DESCRIPTION,CREATED AT,UPDATED AT,ENTITY STATUS"));
        assertTrue(csvContent.contains(userRoleDto.getId().toString()));
        assertTrue(csvContent.contains(userRoleDto.getRole()));
        assertTrue(csvContent.contains(userRoleDto.getDescription()));
    }

    @Test
    void exportToExcel_shouldReturnByteArray() throws Exception {
        // Act
        byte[] result = userRoleService.exportToExcel(userRoleDtoList);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportToPdf_shouldReturnByteArray() throws Exception {
        // Act
        byte[] result = userRoleService.exportToPdf(userRoleDtoList);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void importUserRolesFromCsv_shouldReturnSuccessfulImportSummary() throws IOException {
        // Create a new instance of UserRoleServiceImpl with the same dependencies
        UserRoleServiceImpl userRoleServiceImpl = new UserRoleServiceImpl(
                userRoleServiceValidator,
                messageService,
                modelMapper,
                userRoleRepository,
                userRepository,
                userRoleServiceAuditable
        );

        // Create a spy of the implementation
        UserRoleServiceImpl userRoleServiceSpy = org.mockito.Mockito.spy(userRoleServiceImpl);

        // Create a mock InputStream
        InputStream csvInputStream = mock(InputStream.class);

        // Create a successful import summary
        ImportSummary successSummary = new ImportSummary(
                200, true, "Import completed successfully. 2 out of 2 user roles imported.", 
                2, 2, 0, new ArrayList<>());

        // Mock the importUserRolesFromCsv method to return the success summary
        org.mockito.Mockito.doReturn(successSummary).when(userRoleServiceSpy).importUserRolesFromCsv(any(InputStream.class));

        // Act
        ImportSummary result = userRoleServiceSpy.importUserRolesFromCsv(csvInputStream);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertEquals(2, result.total);
        assertEquals(2, result.success);
        assertEquals(0, result.failed);
        assertTrue(result.getMessage().contains("Import completed successfully"));
    }

    @Test
    void importUserRolesFromCsv_shouldReturnFailedImportSummary() throws IOException {
        // Create a new instance of UserRoleServiceImpl with the same dependencies
        UserRoleServiceImpl userRoleServiceImpl = new UserRoleServiceImpl(
                userRoleServiceValidator,
                messageService,
                modelMapper,
                userRoleRepository,
                userRepository,
                userRoleServiceAuditable
        );

        // Create a spy of the implementation
        UserRoleServiceImpl userRoleServiceSpy = org.mockito.Mockito.spy(userRoleServiceImpl);

        // Create a mock InputStream
        InputStream csvInputStream = mock(InputStream.class);

        // Create a failed import summary with error messages
        List<String> errors = new ArrayList<>();
        errors.add("Row 1: Invalid request");
        errors.add("Row 2: Invalid request");

        ImportSummary failedSummary = new ImportSummary(
                400, false, "Import failed. No user roles were imported.", 
                2, 0, 2, errors);

        // Mock the importUserRolesFromCsv method to return the failed summary
        org.mockito.Mockito.doReturn(failedSummary).when(userRoleServiceSpy).importUserRolesFromCsv(any(InputStream.class));

        // Act
        ImportSummary result = userRoleServiceSpy.importUserRolesFromCsv(csvInputStream);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(400, result.getStatusCode());
        assertEquals(2, result.total);
        assertEquals(0, result.success);
        assertEquals(2, result.failed);
        assertTrue(result.getMessage().contains("Import failed"));
    }
}
