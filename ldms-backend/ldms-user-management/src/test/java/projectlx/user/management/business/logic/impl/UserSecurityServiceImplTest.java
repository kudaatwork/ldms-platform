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
import projectlx.user.management.business.auditable.api.UserSecurityServiceAuditable;
import projectlx.user.management.business.logic.api.UserSecurityService;
import projectlx.user.management.business.validator.api.UserSecurityServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserSecurity;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.repository.UserSecurityRepository;
import projectlx.user.management.utils.dtos.UserSecurityDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserSecurityRequest;
import projectlx.user.management.utils.requests.EditUserSecurityRequest;
import projectlx.user.management.utils.requests.UserSecurityMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserSecurityResponse;

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

class UserSecurityServiceImplTest {

    private UserSecurityService userSecurityService;
    private UserSecurityServiceValidator userSecurityServiceValidator;
    private UserSecurityRepository userSecurityRepository;
    private UserRepository userRepository;
    private UserSecurityServiceAuditable userSecurityServiceAuditable;
    private MessageService messageService;
    private ModelMapper modelMapper;
    private Configuration configurationMock;

    private final Locale locale = Locale.ENGLISH;
    private final String username = "SYSTEM";

    private CreateUserSecurityRequest createUserSecurityRequest;
    private EditUserSecurityRequest editUserSecurityRequest;
    private UserSecurityMultipleFiltersRequest userSecurityMultipleFiltersRequest;
    private UserSecurity userSecurity;
    private UserSecurityDto userSecurityDto;
    private List<UserSecurity> userSecurityList;
    private List<UserSecurityDto> userSecurityDtoList;
    private Long userSecurityId;
    private Long userId;
    private User user;
    private Page<UserSecurity> userSecurityPage;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        userSecurityServiceValidator = mock(UserSecurityServiceValidator.class);
        userSecurityRepository = mock(UserSecurityRepository.class);
        userRepository = mock(UserRepository.class);
        userSecurityServiceAuditable = mock(UserSecurityServiceAuditable.class);
        messageService = mock(MessageService.class);
        modelMapper = mock(ModelMapper.class);
        configurationMock = mock(Configuration.class);

        // Initialize service with mocks
        userSecurityService = new UserSecurityServiceImpl(
                userSecurityServiceValidator,
                messageService,
                userSecurityRepository,
                userRepository,
                modelMapper,
                userSecurityServiceAuditable
        );

        // Common mock setup for ModelMapper configuration
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);

        // Initialize test data
        userSecurityId = 1L;
        userId = 2L;

        user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setEntityStatus(EntityStatus.ACTIVE);

        userSecurity = new UserSecurity();
        userSecurity.setId(userSecurityId);
        userSecurity.setSecurityQuestion_1("What is your mother's maiden name?");
        userSecurity.setSecurityAnswer_1("Smith");
        userSecurity.setSecurityQuestion_2("What was your first pet's name?");
        userSecurity.setSecurityAnswer_2("Fluffy");
        userSecurity.setIsTwoFactorEnabled(true);
        userSecurity.setTwoFactorAuthSecret("SECRET123");
        userSecurity.setCreatedAt(LocalDateTime.now());
        userSecurity.setUpdatedAt(LocalDateTime.now());
        userSecurity.setEntityStatus(EntityStatus.ACTIVE);
        userSecurity.setUser(user);

        userSecurityDto = new UserSecurityDto();
        userSecurityDto.setId(userSecurityId);
        userSecurityDto.setSecurityQuestion_1("What is your mother's maiden name?");
        userSecurityDto.setSecurityAnswer_1("Smith");
        userSecurityDto.setSecurityQuestion_2("What was your first pet's name?");
        userSecurityDto.setSecurityAnswer_2("Fluffy");
        userSecurityDto.setIsTwoFactorEnabled(true);
        userSecurityDto.setTwoFactorAuthSecret("SECRET123");

        createUserSecurityRequest = new CreateUserSecurityRequest();
        createUserSecurityRequest.setUserId(userId);
        createUserSecurityRequest.setSecurityQuestion_1("What is your mother's maiden name?");
        createUserSecurityRequest.setSecurityAnswer_1("Smith");
        createUserSecurityRequest.setSecurityQuestion_2("What was your first pet's name?");
        createUserSecurityRequest.setSecurityAnswer_2("Fluffy");
        createUserSecurityRequest.setIsTwoFactorEnabled(true);
        createUserSecurityRequest.setTwoFactorAuthSecret("SECRET123");

        editUserSecurityRequest = new EditUserSecurityRequest();
        editUserSecurityRequest.setId(userSecurityId);
        editUserSecurityRequest.setSecurityQuestion_1("What is your mother's maiden name?");
        editUserSecurityRequest.setSecurityAnswer_1("Johnson");
        editUserSecurityRequest.setSecurityQuestion_2("What was your first pet's name?");
        editUserSecurityRequest.setSecurityAnswer_2("Rex");
        editUserSecurityRequest.setIsTwoFactorEnabled(false);
        editUserSecurityRequest.setTwoFactorAuthSecret("NEWSECRET456");

        userSecurityMultipleFiltersRequest = new UserSecurityMultipleFiltersRequest();
        userSecurityMultipleFiltersRequest.setIsTwoFactorEnabled(true);
        userSecurityMultipleFiltersRequest.setSearchValue("Smith");
        userSecurityMultipleFiltersRequest.setPage(0);
        userSecurityMultipleFiltersRequest.setSize(10);

        userSecurityList = new ArrayList<>();
        userSecurityList.add(userSecurity);

        userSecurityDtoList = new ArrayList<>();
        userSecurityDtoList.add(userSecurityDto);

        Pageable pageable = PageRequest.of(0, 10);
        userSecurityPage = new PageImpl<>(userSecurityList, pageable, 1);
    }

    @Test
    void create_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Arrange
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Error message");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);

        when(userSecurityServiceValidator.isCreateUserSecurityRequestValid(any(CreateUserSecurityRequest.class), eq(locale))).thenReturn(validatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_CREATE_USER_SECURITY_INVALID_REQUEST.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid request");

        // Act
        UserSecurityResponse response = userSecurityService.create(createUserSecurityRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid request", response.getMessage());
        assertNotNull(response.getErrorMessages());
        assertEquals(1, response.getErrorMessages().size());
        assertEquals("Error message", response.getErrorMessages().get(0));
    }

    @Test
    void create_shouldReturnFalseAnd400IfUserNotFound() {
        // Arrange
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userSecurityServiceValidator.isCreateUserSecurityRequestValid(any(CreateUserSecurityRequest.class), eq(locale))).thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User not found");

        // Act
        UserSecurityResponse response = userSecurityService.create(createUserSecurityRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User not found", response.getMessage());
    }

    @Test
    void create_shouldReturnTrueAnd201ForValidRequest() {
        // Arrange
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userSecurityServiceValidator.isCreateUserSecurityRequestValid(any(CreateUserSecurityRequest.class), eq(locale))).thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(user));
        when(modelMapper.map(any(CreateUserSecurityRequest.class), eq(UserSecurity.class))).thenReturn(userSecurity);
        when(userSecurityServiceAuditable.create(any(UserSecurity.class), eq(locale), eq(username))).thenReturn(userSecurity);
        when(modelMapper.map(any(UserSecurity.class), eq(UserSecurityDto.class))).thenReturn(userSecurityDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_CREATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User security created successfully");

        // Act
        UserSecurityResponse response = userSecurityService.create(createUserSecurityRequest, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User security created successfully", response.getMessage());
        assertNotNull(response.getUserSecurityDto());
        assertEquals(userSecurityId, response.getUserSecurityDto().getId());
    }

    @Test
    void findById_shouldReturnFalseAnd400IfIdIsInvalid() {
        // Arrange
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid ID error");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);

        when(userSecurityServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid ID");

        // Act
        UserSecurityResponse response = userSecurityService.findById(userSecurityId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid ID", response.getMessage());
        assertNotNull(response.getErrorMessages());
        assertEquals(1, response.getErrorMessages().size());
        assertEquals("Invalid ID error", response.getErrorMessages().get(0));
    }

    @Test
    void findById_shouldReturnFalseAnd404IfUserSecurityNotFound() {
        // Arrange
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userSecurityServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(userSecurityRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User security not found");

        // Act
        UserSecurityResponse response = userSecurityService.findById(userSecurityId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User security not found", response.getMessage());
    }

    @Test
    void findById_shouldReturnTrueAnd200ForValidId() {
        // Arrange
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userSecurityServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(userSecurityRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userSecurity));
        when(modelMapper.map(any(UserSecurity.class), eq(UserSecurityDto.class))).thenReturn(userSecurityDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User security retrieved successfully");

        // Act
        UserSecurityResponse response = userSecurityService.findById(userSecurityId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User security retrieved successfully", response.getMessage());
        assertNotNull(response.getUserSecurityDto());
        assertEquals(userSecurityId, response.getUserSecurityDto().getId());
    }

    @Test
    void findAllAsList_shouldReturnFalseAnd404IfNoUserSecurityFound() {
        // Arrange
        when(userSecurityRepository.findByEntityStatusNot(eq(EntityStatus.DELETED)))
                .thenReturn(Collections.emptyList());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("No user security found");

        // Act
        UserSecurityResponse response = userSecurityService.findAllAsList(username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("No user security found", response.getMessage());
    }

    @Test
    void findAllAsList_shouldReturnTrueAnd200WithUserSecurityList() {
        // Arrange
        when(userSecurityRepository.findByEntityStatusNot(eq(EntityStatus.DELETED)))
                .thenReturn(userSecurityList);

        // Create a TypeToken for the correct type
        TypeToken<List<UserSecurityDto>> typeToken = new TypeToken<List<UserSecurityDto>>() {};
        when(modelMapper.map(any(), eq(typeToken.getType()))).thenReturn(userSecurityDtoList);

        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User security retrieved successfully");

        // Act
        UserSecurityResponse response = userSecurityService.findAllAsList(username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User security retrieved successfully", response.getMessage());
        assertNotNull(response.getUserSecurityDtoList());
        assertEquals(1, response.getUserSecurityDtoList().size());
    }

    @Test
    void update_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Arrange
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid request error");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);
        when(userSecurityServiceValidator.isRequestValidForEditing(any(EditUserSecurityRequest.class), eq(locale))).thenReturn(validatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_UPDATE_USER_SECURITY_INVALID_REQUEST.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid update request");

        // Act
        UserSecurityResponse response = userSecurityService.update(editUserSecurityRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid update request", response.getMessage());
    }

    @Test
    void update_shouldReturnFalseAnd400IfUserSecurityNotFound() {
        // Arrange
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userSecurityServiceValidator.isRequestValidForEditing(any(EditUserSecurityRequest.class), eq(locale))).thenReturn(validatorDto);
        when(userSecurityRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User security not found");

        // Act
        UserSecurityResponse response = userSecurityService.update(editUserSecurityRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User security not found", response.getMessage());
    }

    @Test
    void update_shouldReturnTrueAnd201ForValidRequest() {
        // Arrange
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userSecurityServiceValidator.isRequestValidForEditing(any(EditUserSecurityRequest.class), eq(locale))).thenReturn(validatorDto);
        when(userSecurityRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userSecurity));
        when(userSecurityRepository.countMatchingSecuritySetup(anyString(), anyString(), anyString(), anyString(), any(EntityStatus.class)))
                .thenReturn(1L); // Only one record with this security setup
        when(userSecurityServiceAuditable.update(any(UserSecurity.class), eq(locale), eq(username))).thenReturn(userSecurity);
        when(modelMapper.map(any(UserSecurity.class), eq(UserSecurityDto.class))).thenReturn(userSecurityDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_UPDATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User security updated successfully");

        // Act
        UserSecurityResponse response = userSecurityService.update(editUserSecurityRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User security updated successfully", response.getMessage());
        assertNotNull(response.getUserSecurityDto());
        assertEquals(userSecurityId, response.getUserSecurityDto().getId());
    }

    @Test
    void update_shouldCreateNewRecordWhenSharedSecuritySetup() {
        // Arrange
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userSecurityServiceValidator.isRequestValidForEditing(any(EditUserSecurityRequest.class), eq(locale))).thenReturn(validatorDto);
        when(userSecurityRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userSecurity));
        when(userSecurityRepository.countMatchingSecuritySetup(anyString(), anyString(), anyString(), anyString(), any(EntityStatus.class)))
                .thenReturn(2L); // Multiple records with this security setup
        when(userSecurityServiceAuditable.update(any(UserSecurity.class), eq(locale), eq(username))).thenReturn(userSecurity);
        when(modelMapper.map(any(UserSecurity.class), eq(UserSecurityDto.class))).thenReturn(userSecurityDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_UPDATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User security updated successfully");

        // Act
        UserSecurityResponse response = userSecurityService.update(editUserSecurityRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(201, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User security updated successfully", response.getMessage());
        assertNotNull(response.getUserSecurityDto());
        assertEquals(userSecurityId, response.getUserSecurityDto().getId());
    }

    @Test
    void delete_shouldReturnFalseAnd400IfIdIsInvalid() {
        // Arrange
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid ID error");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);
        when(userSecurityServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid ID");

        // Act
        UserSecurityResponse response = userSecurityService.delete(userSecurityId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid ID", response.getMessage());
    }

    @Test
    void delete_shouldReturnFalseAnd404IfUserSecurityNotFound() {
        // Arrange
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userSecurityServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(userSecurityRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User security not found");

        // Act
        UserSecurityResponse response = userSecurityService.delete(userSecurityId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("User security not found", response.getMessage());
    }

    @Test
    void delete_shouldReturnTrueAnd200ForValidRequest() {
        // Arrange
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userSecurityServiceValidator.isIdValid(anyLong(), eq(locale))).thenReturn(validatorDto);
        when(userSecurityRepository.findByIdAndEntityStatusNot(anyLong(), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userSecurity));
        when(userSecurityServiceAuditable.delete(any(UserSecurity.class), eq(locale))).thenReturn(userSecurity);
        when(modelMapper.map(any(UserSecurity.class), eq(UserSecurityDto.class))).thenReturn(userSecurityDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_DELETED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User security deleted successfully");

        // Act
        UserSecurityResponse response = userSecurityService.delete(userSecurityId, locale, username);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User security deleted successfully", response.getMessage());
        assertNotNull(response.getUserSecurityDto());
        assertEquals(userSecurityId, response.getUserSecurityDto().getId());
        verify(userSecurityServiceAuditable, times(1)).delete(any(UserSecurity.class), eq(locale));
    }

    @Test
    void findByMultipleFilters_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Arrange
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid filters request error");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);
        when(userSecurityServiceValidator.isRequestValidToRetrieveUserSecurityByMultipleFilters(any(UserSecurityMultipleFiltersRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_INVALID_MULTIPLE_FILTERS_REQUEST.getCode()), any(String[].class), eq(locale)))
                .thenReturn("Invalid filters request");

        // Act
        UserSecurityResponse response = userSecurityService.findByMultipleFilters(userSecurityMultipleFiltersRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("Invalid filters request", response.getMessage());
    }

    @Test
    void findByMultipleFilters_shouldReturnFalseAnd404IfNoUserSecurityFound() {
        // Arrange
        ValidatorDto validRequestDto = new ValidatorDto(true, null, null);
        ValidatorDto validBooleanDto = new ValidatorDto(true, null, null);
        ValidatorDto validStringDto = new ValidatorDto(true, null, null);
        when(userSecurityServiceValidator.isRequestValidToRetrieveUserSecurityByMultipleFilters(any(UserSecurityMultipleFiltersRequest.class), eq(locale)))
                .thenReturn(validRequestDto);
        when(userSecurityServiceValidator.isBooleanValid(any(), eq(locale))).thenReturn(validBooleanDto);
        when(userSecurityServiceValidator.isStringValid(anyString(), eq(locale))).thenReturn(validStringDto);
        when(userSecurityRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_NOT_FOUND.getCode()), any(String[].class), eq(locale)))
                .thenReturn("No user security found");

        // Act
        UserSecurityResponse response = userSecurityService.findByMultipleFilters(userSecurityMultipleFiltersRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertEquals("No user security found", response.getMessage());
    }

    @Test
    void findByMultipleFilters_shouldReturnTrueAnd200WithUserSecurityPage() {
        // Arrange
        ValidatorDto validRequestDto = new ValidatorDto(true, null, null);
        ValidatorDto validBooleanDto = new ValidatorDto(true, null, null);
        ValidatorDto validStringDto = new ValidatorDto(true, null, null);
        when(userSecurityServiceValidator.isRequestValidToRetrieveUserSecurityByMultipleFilters(any(UserSecurityMultipleFiltersRequest.class), eq(locale)))
                .thenReturn(validRequestDto);
        when(userSecurityServiceValidator.isBooleanValid(any(), eq(locale))).thenReturn(validBooleanDto);
        when(userSecurityServiceValidator.isStringValid(anyString(), eq(locale))).thenReturn(validStringDto);
        when(userSecurityRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(userSecurityPage);
        when(modelMapper.map(any(UserSecurity.class), eq(UserSecurityDto.class))).thenReturn(userSecurityDto);
        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_SECURITY_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale)))
                .thenReturn("User security retrieved successfully");

        // Act
        UserSecurityResponse response = userSecurityService.findByMultipleFilters(userSecurityMultipleFiltersRequest, username, locale);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("User security retrieved successfully", response.getMessage());
        assertNotNull(response.getUserSecurityDtoPage());
        assertEquals(1, response.getUserSecurityDtoPage().getTotalElements());
    }
}
