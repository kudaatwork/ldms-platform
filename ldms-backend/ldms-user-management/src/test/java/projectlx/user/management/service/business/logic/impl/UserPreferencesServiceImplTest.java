package projectlx.user.management.service.business.logic.impl;

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
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.auditable.api.UserPreferencesServiceAuditable;
import projectlx.user.management.service.business.logic.api.UserPreferencesService;
import projectlx.user.management.service.business.validator.api.UserPreferencesServiceValidator;
import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.User;
import projectlx.user.management.service.model.UserPreferences;
import projectlx.user.management.service.repository.UserPreferencesRepository;
import projectlx.user.management.service.repository.UserRepository;
import projectlx.user.management.service.utils.dtos.UserPreferencesDto;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.CreateUserPreferencesRequest;
import projectlx.user.management.service.utils.requests.EditUserPreferencesRequest;
import projectlx.user.management.service.utils.requests.UserPreferencesMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserPreferencesResponse;

import java.lang.reflect.Type;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPreferencesServiceImplTest {

    private UserPreferencesService userPreferencesService;
    private UserPreferencesServiceValidator userPreferencesServiceValidator;
    private UserPreferencesRepository userPreferencesRepository;
    private UserRepository userRepository;
    private UserPreferencesServiceAuditable userPreferencesServiceAuditable;
    private MessageService messageService;
    private ModelMapper modelMapper;
    private Configuration configurationMock;

    private final Locale locale = Locale.ENGLISH;
    private final String username = "SYSTEM";

    private CreateUserPreferencesRequest createUserPreferencesRequest;
    private EditUserPreferencesRequest editUserPreferencesRequest;
    private UserPreferencesMultipleFiltersRequest userPreferencesMultipleFiltersRequest;
    private UserPreferences userPreferences;
    private UserPreferencesDto userPreferencesDto;
    private List<UserPreferences> userPreferencesList;
    private List<UserPreferencesDto> userPreferencesDtoList;
    private User user;
    private Long userPreferencesId;
    private Page<UserPreferences> userPreferencesPage;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        userPreferencesServiceValidator = mock(UserPreferencesServiceValidator.class);
        userPreferencesRepository = mock(UserPreferencesRepository.class);
        userRepository = mock(UserRepository.class);
        userPreferencesServiceAuditable = mock(UserPreferencesServiceAuditable.class);
        messageService = mock(MessageService.class);
        modelMapper = mock(ModelMapper.class);
        configurationMock = mock(Configuration.class);

        // Initialize service with mocks
        userPreferencesService = new UserPreferencesServiceImpl(
                userPreferencesServiceValidator,
                messageService,
                userPreferencesRepository,
                userRepository,
                modelMapper,
                userPreferencesServiceAuditable
        );

        // Common mock setup for ModelMapper configuration
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);

        // Initialize test data
        userPreferencesId = 1L;

        user = new User();
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEntityStatus(EntityStatus.ACTIVE);

        userPreferences = new UserPreferences();
        userPreferences.setId(userPreferencesId);
        userPreferences.setPreferredLanguage("en");
        userPreferences.setTimezone("UTC");
        userPreferences.setUser(user);
        userPreferences.setEntityStatus(EntityStatus.ACTIVE);
        userPreferences.setCreatedAt(LocalDateTime.now());
        userPreferences.setUpdatedAt(LocalDateTime.now());

        userPreferencesList = new ArrayList<>();
        userPreferencesList.add(userPreferences);

        userPreferencesDto = new UserPreferencesDto();
        userPreferencesDto.setId(userPreferencesId);
        userPreferencesDto.setPreferredLanguage("en");
        userPreferencesDto.setTimezone("UTC");

        userPreferencesDtoList = new ArrayList<>();
        userPreferencesDtoList.add(userPreferencesDto);

        createUserPreferencesRequest = new CreateUserPreferencesRequest();
        createUserPreferencesRequest.setUserId(1L);
        createUserPreferencesRequest.setPreferredLanguage("en");
        createUserPreferencesRequest.setTimezone("UTC");

        editUserPreferencesRequest = new EditUserPreferencesRequest();
        editUserPreferencesRequest.setId(userPreferencesId);
        editUserPreferencesRequest.setPreferredLanguage("fr");
        editUserPreferencesRequest.setTimezone("CET");
        editUserPreferencesRequest.setUserId(1L);

        userPreferencesMultipleFiltersRequest = new UserPreferencesMultipleFiltersRequest();
        userPreferencesMultipleFiltersRequest.setPage(0);
        userPreferencesMultipleFiltersRequest.setSize(10);
        userPreferencesMultipleFiltersRequest.setPreferredLanguage("en");
        userPreferencesMultipleFiltersRequest.setTimezone("UTC");

        // Initialize page data
        List<UserPreferences> singleList = Collections.singletonList(userPreferences);
        Pageable pageable = PageRequest.of(0, 10);
        userPreferencesPage = new PageImpl<>(singleList, pageable, singleList.size());
    }

    @Test
    public void create_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Create a ValidatorDto with success=false and some error messages
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Error message 1");
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(false, null, errorMessages);

        when(userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(any(CreateUserPreferencesRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Invalid request");

        UserPreferencesResponse response = userPreferencesService.create(createUserPreferencesRequest, locale, username);

        verify(userPreferencesServiceValidator, times(1))
                .isCreateUserPreferencesRequestValid(any(CreateUserPreferencesRequest.class), any(Locale.class));
        verify(userRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPreferencesServiceAuditable, times(0))
                .create(any(UserPreferences.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getErrorMessages());
        assertEquals(errorMessages, response.getErrorMessages());
    }

    @Test
    public void create_shouldReturnFalseAnd400IfUserNotFound() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);

        when(userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(any(CreateUserPreferencesRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("User not found");

        UserPreferencesResponse response = userPreferencesService.create(createUserPreferencesRequest, locale, username);

        verify(userPreferencesServiceValidator, times(1))
                .isCreateUserPreferencesRequestValid(any(CreateUserPreferencesRequest.class), any(Locale.class));
        verify(userRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPreferencesServiceAuditable, times(0))
                .create(any(UserPreferences.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void create_shouldReturnFalseAnd400IfUserPreferencesAlreadyExists() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);

        when(userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(any(CreateUserPreferencesRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(user));
        when(userPreferencesRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userPreferences));
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("User preferences already exist");

        UserPreferencesResponse response = userPreferencesService.create(createUserPreferencesRequest, locale, username);

        verify(userPreferencesServiceValidator, times(1))
                .isCreateUserPreferencesRequestValid(any(CreateUserPreferencesRequest.class), any(Locale.class));
        verify(userRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPreferencesRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPreferencesServiceAuditable, times(0))
                .create(any(UserPreferences.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void create_shouldReturnTrueAnd201ForValidRequest() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);

        when(userPreferencesServiceValidator.isCreateUserPreferencesRequestValid(any(CreateUserPreferencesRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(user));
        when(userPreferencesRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());

        // Stub ModelMapper configuration chain
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);
        when(configurationMock.setMatchingStrategy(any())).thenReturn(configurationMock);

        // Stub ModelMapper mappings
        when(modelMapper.map(any(CreateUserPreferencesRequest.class), eq(UserPreferences.class)))
                .thenReturn(userPreferences);
        when(modelMapper.map(any(UserPreferences.class), eq(UserPreferencesDto.class)))
                .thenReturn(userPreferencesDto);

        when(userPreferencesServiceAuditable.create(any(UserPreferences.class), any(Locale.class), anyString()))
                .thenReturn(userPreferences);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Success message");

        UserPreferencesResponse response = userPreferencesService.create(createUserPreferencesRequest, locale, username);

        verify(userPreferencesServiceValidator, times(1))
                .isCreateUserPreferencesRequestValid(any(CreateUserPreferencesRequest.class), any(Locale.class));
        verify(userRepository, times(1))
                .findByIdAndEntityStatusNot(createUserPreferencesRequest.getUserId(), EntityStatus.DELETED);
        verify(userPreferencesRepository, times(1))
                .findByIdAndEntityStatusNot(createUserPreferencesRequest.getUserId(), EntityStatus.DELETED);
        verify(modelMapper, times(1))
                .map(createUserPreferencesRequest, UserPreferences.class);
        verify(userPreferencesServiceAuditable, times(1))
                .create(any(UserPreferences.class), any(Locale.class), anyString());
        verify(modelMapper, times(1))
                .map(userPreferences, UserPreferencesDto.class);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getUserPreferencesDto());
    }

    @Test
    public void findById_shouldReturnFalseAnd400IfIdIsInvalid() {
        // Create a ValidatorDto with success=false and some error messages
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid ID error");
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(false, null, errorMessages);

        when(userPreferencesServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Invalid ID");

        UserPreferencesResponse response = userPreferencesService.findById(userPreferencesId, locale, username);

        verify(userPreferencesServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userPreferencesRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getErrorMessages());
        assertEquals(errorMessages, response.getErrorMessages());
    }

    @Test
    public void findById_shouldReturnFalseAnd404IfUserPreferencesNotFound() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);

        when(userPreferencesServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userPreferencesRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("User preferences not found");

        UserPreferencesResponse response = userPreferencesService.findById(userPreferencesId, locale, username);

        verify(userPreferencesServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userPreferencesRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findById_shouldReturnTrueAnd200ForValidId() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);

        when(userPreferencesServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userPreferencesRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userPreferences));
        when(modelMapper.map(any(UserPreferences.class), eq(UserPreferencesDto.class)))
                .thenReturn(userPreferencesDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Success message");

        UserPreferencesResponse response = userPreferencesService.findById(userPreferencesId, locale, username);

        verify(userPreferencesServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userPreferencesRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(modelMapper, times(1))
                .map(userPreferences, UserPreferencesDto.class);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserPreferencesDto());
    }

    @Test
    public void findAllAsList_shouldReturnFalseAnd404IfNoUserPreferencesFound() {
        when(userPreferencesRepository.findByEntityStatusNot(any(EntityStatus.class)))
                .thenReturn(Collections.emptyList());
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("No user preferences found");

        UserPreferencesResponse response = userPreferencesService.findAllAsList(username, locale);

        verify(userPreferencesRepository, times(1))
                .findByEntityStatusNot(EntityStatus.DELETED);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findAllAsList_shouldReturnTrueAnd200WithUserPreferencesList() {
        // Use the same TypeToken as the implementation
        Type listType = new TypeToken<List<UserPreferencesDto>>() {}.getType();

        // Stub repository to return a non-empty list
        when(userPreferencesRepository.findByEntityStatusNot(EntityStatus.DELETED))
                .thenReturn(userPreferencesList);

        // Stub ModelMapper to return our DTO list when given that same TypeToken
        when(modelMapper.map(eq(userPreferencesList), eq(listType)))
                .thenReturn(userPreferencesDtoList);

        // Stub the "retrieved successfully" message using the exact I18 code
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_PREFERENCES_RETRIEVED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("User preferences retrieved successfully");

        // Act
        UserPreferencesResponse response = userPreferencesService.findAllAsList(username, locale);

        // Verify that findByEntityStatusNot was called once with DELETED
        verify(userPreferencesRepository, times(1))
                .findByEntityStatusNot(EntityStatus.DELETED);

        // Verify that ModelMapper.map was called with the same listType
        verify(modelMapper, times(1))
                .map(userPreferencesList, listType);

        // Verify that messageService.getMessage was called with the correct I18 code and locale
        verify(messageService, times(1))
                .getMessage(
                        I18Code.MESSAGE_USER_PREFERENCES_RETRIEVED_SUCCESSFULLY.getCode(),
                        new String[]{},
                        locale
                );

        // Assertions
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserPreferencesDtoList());
        assertFalse(response.getUserPreferencesDtoList().isEmpty());
        assertEquals(1, response.getUserPreferencesDtoList().size());
    }

    @Test
    public void update_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Create a ValidatorDto with success=false and some error messages
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid request error");
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(false, null, errorMessages);

        when(userPreferencesServiceValidator.isRequestValidForEditing(any(EditUserPreferencesRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Invalid request");

        UserPreferencesResponse response = userPreferencesService.update(editUserPreferencesRequest, username, locale);

        verify(userPreferencesServiceValidator, times(1))
                .isRequestValidForEditing(any(EditUserPreferencesRequest.class), any(Locale.class));
        verify(userPreferencesRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPreferencesServiceAuditable, times(0))
                .update(any(UserPreferences.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getErrorMessages());
        assertEquals(errorMessages, response.getErrorMessages());
    }

    @Test
    public void update_shouldReturnFalseAnd400IfUserPreferencesNotFound() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);

        when(userPreferencesServiceValidator.isRequestValidForEditing(any(EditUserPreferencesRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userPreferencesRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("User preferences not found");

        UserPreferencesResponse response = userPreferencesService.update(editUserPreferencesRequest, username, locale);

        verify(userPreferencesServiceValidator, times(1))
                .isRequestValidForEditing(any(EditUserPreferencesRequest.class), any(Locale.class));
        verify(userPreferencesRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPreferencesServiceAuditable, times(0))
                .update(any(UserPreferences.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void update_shouldReturnTrueAnd201ForValidRequest() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);

        when(userPreferencesServiceValidator.isRequestValidForEditing(any(EditUserPreferencesRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userPreferencesRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userPreferences));
        when(userPreferencesRepository.countByPreferredLanguageAndTimezoneAndEntityStatusNot(
                anyString(), anyString(), any(EntityStatus.class)))
                .thenReturn(1L);
        when(userPreferencesServiceAuditable.update(any(UserPreferences.class), any(Locale.class), anyString()))
                .thenReturn(userPreferences);
        when(modelMapper.map(any(UserPreferences.class), eq(UserPreferencesDto.class)))
                .thenReturn(userPreferencesDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Success message");

        UserPreferencesResponse response = userPreferencesService.update(editUserPreferencesRequest, username, locale);

        verify(userPreferencesServiceValidator, times(1))
                .isRequestValidForEditing(any(EditUserPreferencesRequest.class), any(Locale.class));
        verify(userPreferencesRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPreferencesRepository, times(1))
                .countByPreferredLanguageAndTimezoneAndEntityStatusNot(anyString(), anyString(), any(EntityStatus.class));
        verify(userPreferencesServiceAuditable, times(1))
                .update(any(UserPreferences.class), any(Locale.class), anyString());
        verify(modelMapper, times(1))
                .map(userPreferences, UserPreferencesDto.class);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getUserPreferencesDto());
    }

    @Test
    public void delete_shouldReturnFalseAnd400IfIdIsInvalid() {
        // Create a ValidatorDto with success=false and some error messages
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid ID error");
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(false, null, errorMessages);

        when(userPreferencesServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Invalid ID");

        UserPreferencesResponse response = userPreferencesService.delete(userPreferencesId, locale, username);

        verify(userPreferencesServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userPreferencesRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPreferencesServiceAuditable, times(0))
                .delete(any(UserPreferences.class), any(Locale.class));
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getErrorMessages());
        assertEquals(errorMessages, response.getErrorMessages());
    }

    @Test
    public void delete_shouldReturnFalseAnd404IfUserPreferencesNotFound() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);

        when(userPreferencesServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userPreferencesRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("User preferences not found");

        UserPreferencesResponse response = userPreferencesService.delete(userPreferencesId, locale, username);

        verify(userPreferencesServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userPreferencesRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPreferencesServiceAuditable, times(0))
                .delete(any(UserPreferences.class), any(Locale.class));
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void delete_shouldReturnTrueAnd200ForValidRequest() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);

        when(userPreferencesServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userPreferencesRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userPreferences));
        when(userPreferencesServiceAuditable.delete(any(UserPreferences.class), any(Locale.class)))
                .thenReturn(userPreferences);
        when(modelMapper.map(any(UserPreferences.class), eq(UserPreferencesDto.class)))
                .thenReturn(userPreferencesDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Delete successful");

        UserPreferencesResponse response = userPreferencesService.delete(userPreferencesId, locale, username);

        verify(userPreferencesServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userPreferencesRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPreferencesServiceAuditable, times(1))
                .delete(any(UserPreferences.class), any(Locale.class));
        verify(modelMapper, times(1))
                .map(userPreferences, UserPreferencesDto.class);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserPreferencesDto());
    }

    @Test
    public void findByMultipleFilters_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Create a ValidatorDto with success=false and some error messages
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid request error");
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(false, null, errorMessages);

        when(userPreferencesServiceValidator.isRequestValidToRetrieveUserPreferencesByMultipleFilters(any(UserPreferencesMultipleFiltersRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Invalid request");

        UserPreferencesResponse response = userPreferencesService.findByMultipleFilters(
                userPreferencesMultipleFiltersRequest, username, locale
        );

        verify(userPreferencesServiceValidator, times(1))
                .isRequestValidToRetrieveUserPreferencesByMultipleFilters(any(UserPreferencesMultipleFiltersRequest.class), any(Locale.class));
        verify(userPreferencesRepository, times(0))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getErrorMessages());
        assertEquals(errorMessages, response.getErrorMessages());
    }

    @Test
    public void findByMultipleFilters_shouldReturnFalseAnd404IfNoUserPreferencesFound() {
        // Create ValidatorDto instances with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto1 = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto2 = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDtoFalse = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(false, null, new ArrayList<>());

        when(userPreferencesServiceValidator.isRequestValidToRetrieveUserPreferencesByMultipleFilters(any(UserPreferencesMultipleFiltersRequest.class), any(Locale.class)))
                .thenReturn(validatorDto1);
        // Handle non-null values
        when(userPreferencesServiceValidator.isStringValid(argThat(arg -> arg != null), any(Locale.class)))
                .thenReturn(validatorDto2);
        // Handle null values
        when(userPreferencesServiceValidator.isStringValid(isNull(), any(Locale.class)))
                .thenReturn(validatorDtoFalse);
        when(userPreferencesRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("No user preferences found");

        UserPreferencesResponse response = userPreferencesService.findByMultipleFilters(
                userPreferencesMultipleFiltersRequest, username, locale
        );

        verify(userPreferencesServiceValidator, times(1))
                .isRequestValidToRetrieveUserPreferencesByMultipleFilters(any(UserPreferencesMultipleFiltersRequest.class), any(Locale.class));
        verify(userPreferencesRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findByMultipleFilters_shouldReturnTrueAnd200WithUserPreferencesPage() {
        // Create ValidatorDto instances with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto1 = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto2 = 
            new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);

        when(userPreferencesServiceValidator.isRequestValidToRetrieveUserPreferencesByMultipleFilters(any(UserPreferencesMultipleFiltersRequest.class), any(Locale.class)))
                .thenReturn(validatorDto1);
        when(userPreferencesServiceValidator.isStringValid(anyString(), any(Locale.class)))
                .thenReturn(validatorDto2);
        when(userPreferencesRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(userPreferencesPage);
        when(modelMapper.map(any(UserPreferences.class), eq(UserPreferencesDto.class)))
                .thenReturn(userPreferencesDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Success message");

        UserPreferencesResponse response = userPreferencesService.findByMultipleFilters(
                userPreferencesMultipleFiltersRequest, username, locale
        );

        verify(userPreferencesServiceValidator, times(1))
                .isRequestValidToRetrieveUserPreferencesByMultipleFilters(any(UserPreferencesMultipleFiltersRequest.class), any(Locale.class));
        verify(userPreferencesRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(modelMapper, times(1))
                .map(userPreferences, UserPreferencesDto.class);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserPreferencesDtoPage());
        assertFalse(response.getUserPreferencesDtoPage().isEmpty());
        assertEquals(1, response.getUserPreferencesDtoPage().getTotalElements());
    }
}
