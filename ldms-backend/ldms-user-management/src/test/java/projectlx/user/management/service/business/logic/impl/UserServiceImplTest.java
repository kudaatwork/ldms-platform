package projectlx.user.management.service.business.logic.impl;

import com.lowagie.text.DocumentException;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import projectlx.co.zw.shared_library.business.logic.impl.TokenService;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import projectlx.user.management.service.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.business.auditable.api.UserAccountServiceAuditable;
import projectlx.user.management.service.business.auditable.api.UserPasswordServiceAuditable;
import projectlx.user.management.service.business.auditable.api.UserPreferencesServiceAuditable;
import projectlx.user.management.service.business.auditable.api.UserSecurityServiceAuditable;
import projectlx.user.management.service.business.logic.api.UserService;
import projectlx.user.management.service.business.logic.api.UserAccountService;
import projectlx.user.management.service.business.logic.api.UserPasswordService;
import projectlx.user.management.service.business.logic.api.UserAddressService;
import projectlx.user.management.service.business.logic.api.UserPreferencesService;
import projectlx.user.management.service.business.logic.api.UserSecurityService;
import projectlx.user.management.service.business.logic.api.UserTypeService;
import projectlx.user.management.service.business.validator.api.UserServiceValidator;
import projectlx.user.management.service.clients.FileUploadServiceClient;
import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.Gender;
import projectlx.user.management.service.model.User;
import projectlx.user.management.service.model.UserAccount;
import projectlx.user.management.service.model.UserPassword;
import projectlx.user.management.service.model.UserPreferences;
import projectlx.user.management.service.model.UserSecurity;
import projectlx.user.management.service.repository.UserRepository;
import projectlx.user.management.service.repository.UserAccountRepository;
import projectlx.user.management.service.repository.UserAddressRepository;
import projectlx.user.management.service.repository.UserPasswordRepository;
import projectlx.user.management.service.repository.UserPreferencesRepository;
import projectlx.user.management.service.repository.UserSecurityRepository;
import projectlx.user.management.service.repository.UserTypeRepository;
import projectlx.user.management.service.utils.dtos.UserDto;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.CreateUserRequest;
import projectlx.user.management.service.utils.requests.EditUserRequest;
import projectlx.user.management.service.utils.requests.UsersMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    private UserService userService = null;
    private UserServiceValidator userServiceValidator = null;
    private MessageService messageService = null;
    private UserRepository userRepository = null;
    private UserAccountRepository userAccountRepository = null;
    private UserAddressRepository userAddressRepository = null;
    private UserPasswordRepository userPasswordRepository = null;
    private UserPreferencesRepository userPreferencesRepository = null;
    private UserSecurityRepository userSecurityRepository = null;
    private UserTypeRepository userTypeRepository = null;
    private ModelMapper modelMapper = null;
    private UserServiceAuditable userServiceAuditable = null;
    private UserAccountServiceAuditable userAccountServiceAuditable = null;
    private UserPasswordServiceAuditable userPasswordServiceAuditable = null;
    private UserPreferencesServiceAuditable userPreferencesServiceAuditable = null;
    private UserSecurityServiceAuditable userSecurityServiceAuditable = null;
    private UserAccountService userAccountService = null;
    private UserPasswordService userPasswordService = null;
    private UserAddressService userAddressService = null;
    private UserPreferencesService userPreferencesService = null;
    private UserSecurityService userSecurityService = null;
    private UserTypeService userTypeService = null;
    private FileUploadServiceClient fileUploadServiceClient = null;
    private ObjectMapper objectMapper = null;
    private Configuration configurationMock = null;
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate = null;
    private TokenService tokenService = null;
    private final Locale locale = Locale.ENGLISH;
    private final String username = "SYSTEM";

    private CreateUserRequest createUserRequest = null;
    private EditUserRequest editUserRequest = null;
    private UsersMultipleFiltersRequest usersMultipleFiltersRequest = null;
    private User user = null;
    private UserPassword userPassword = null;
    private UserSecurity userSecurity = null;
    private UserPreferences userPreferences = null;
    private UserDto userDto = null;
    private Long userId = null;
    private Page<User> userPage = null;

    @BeforeEach
    void setUp() {
        userServiceValidator = mock(UserServiceValidator.class);
        messageService = mock(MessageService.class);
        userRepository = mock(UserRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        userAddressRepository = mock(UserAddressRepository.class);
        userPasswordRepository = mock(UserPasswordRepository.class);
        userPreferencesRepository = mock(UserPreferencesRepository.class);
        userSecurityRepository = mock(UserSecurityRepository.class);
        userTypeRepository = mock(UserTypeRepository.class);
        modelMapper = mock(ModelMapper.class);
        userServiceAuditable = mock(UserServiceAuditable.class);
        userAccountServiceAuditable = mock(UserAccountServiceAuditable.class);
        userPasswordServiceAuditable = mock(UserPasswordServiceAuditable.class);
        userPreferencesServiceAuditable = mock(UserPreferencesServiceAuditable.class);
        userSecurityServiceAuditable = mock(UserSecurityServiceAuditable.class);
        userAccountService = mock(UserAccountService.class);
        userPasswordService = mock(UserPasswordService.class);
        userAddressService = mock(UserAddressService.class);
        userPreferencesService = mock(UserPreferencesService.class);
        userSecurityService = mock(UserSecurityService.class);
        userTypeService = mock(UserTypeService.class);
        fileUploadServiceClient = mock(FileUploadServiceClient.class);
        objectMapper = mock(ObjectMapper.class);
        configurationMock = mock(Configuration.class);
        rabbitTemplate = mock(org.springframework.amqp.rabbit.core.RabbitTemplate.class);
        tokenService = mock(TokenService.class);

        userService = new UserServiceImpl(
                userServiceValidator,
                messageService,
                userRepository,
                userAccountRepository,
                userAddressRepository,
                userPasswordRepository,
                userPreferencesRepository,
                userSecurityRepository,
                userTypeRepository,
                modelMapper,
                userServiceAuditable,
                userAccountServiceAuditable,
                userPasswordServiceAuditable,
                userPreferencesServiceAuditable,
                userSecurityServiceAuditable,
                userAccountService,
                userPasswordService,
                userAddressService,
                userPreferencesService,
                userSecurityService,
                userTypeService,
                fileUploadServiceClient,
                rabbitTemplate,
                tokenService
        );

        // Set the objectMapper field using reflection
        try {
            java.lang.reflect.Field field = UserServiceImpl.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(userService, objectMapper);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize test data
        userId = 1L;

        createUserRequest = new CreateUserRequest();
        createUserRequest.setFirstName("John");
        createUserRequest.setLastName("Doe");
        createUserRequest.setEmail("john.doe@example.com");
        createUserRequest.setPhoneNumber("+1234567890");
        createUserRequest.setPassword("Password123!");
        createUserRequest.setGender(Gender.MALE.name());

        editUserRequest = new EditUserRequest();
        editUserRequest.setId(userId);
        editUserRequest.setFirstName("John");
        editUserRequest.setLastName("Smith");
        editUserRequest.setEmail("john.smith@example.com");
        editUserRequest.setPhoneNumber("+1234567890");
        editUserRequest.setGender(Gender.MALE.name());

        usersMultipleFiltersRequest = new UsersMultipleFiltersRequest();
        usersMultipleFiltersRequest.setPage(0);
        usersMultipleFiltersRequest.setSize(10);
        usersMultipleFiltersRequest.setFirstName("John");
        usersMultipleFiltersRequest.setLastName("Doe");
        usersMultipleFiltersRequest.setEmail("john.doe@example.com");
        usersMultipleFiltersRequest.setSearchValue("john");

        user = new User();
        user.setId(userId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPhoneNumber("+1234567890");
        user.setGender(Gender.MALE);
        user.setEntityStatus(EntityStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDateOfBirth(new Date(90, 0, 1)); // 1990-01-01

        userPassword = new UserPassword();
        userPassword.setId(userId);
        userPassword.setPassword("Password123!");
        userPassword.setEntityStatus(EntityStatus.ACTIVE);
        userPassword.setUser(user);

        userSecurity = new UserSecurity();
        userSecurity.setId(userId);
        userSecurity.setEntityStatus(EntityStatus.ACTIVE);
        userSecurity.setUser(user);

        userPreferences = new UserPreferences();
        userPreferences.setId(userId);
        userPreferences.setEntityStatus(EntityStatus.ACTIVE);
        userPreferences.setUser(user);

        // Create and set UserAccount to avoid NullPointerException in delete test
        UserAccount userAccount = new UserAccount();
        userAccount.setId(1L);
        userAccount.setPhoneNumber("+1234567890");
        userAccount.setAccountNumber("ACC123456");
        userAccount.setIsAccountLocked(false);
        userAccount.setEntityStatus(EntityStatus.ACTIVE);
        userAccount.setUser(user);
        user.setUserAccount(userAccount);

        userDto = new UserDto();
        userDto.setId(userId);
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setEmail("john.doe@example.com");
        userDto.setPhoneNumber("+1234567890");
        userDto.setGender(Gender.MALE);

        List<User> userList = new ArrayList<>();
        userList.add(user);
        Pageable pageable = PageRequest.of(0, 10);
        userPage = new PageImpl<>(userList, pageable, userList.size());

        // Common mock setups
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);
    }

    @Test
    public void create_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Create a ValidatorDto with success=false
        List<String> errorMessages = List.of("Invalid request");
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(false, null, errorMessages);
        when(userServiceValidator.isCreateUserRequestValid(any(CreateUserRequest.class), any(Locale.class))).thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Invalid request");

        UserResponse response = userService.create(createUserRequest, locale, username);

        verify(userServiceValidator, times(1)).isCreateUserRequestValid(any(CreateUserRequest.class), any(Locale.class));
        verify(userRepository, times(0)).save(any(User.class));
        verify(userServiceAuditable, times(0)).create(any(User.class), any(Locale.class), anyString());

        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
        assertNotNull(response.getErrorMessages());
        assertEquals(errorMessages, response.getErrorMessages());
    }

    @Test
    public void create_shouldReturnFalseAnd400IfPasswordIsInvalid() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);
        when(userServiceValidator.isCreateUserRequestValid(any(CreateUserRequest.class), any(Locale.class))).thenReturn(validatorDto);
        when(userServiceValidator.isPasswordValid(anyString())).thenReturn(false);
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Invalid password");

        UserResponse response = userService.create(createUserRequest, locale, username);

        verify(userServiceValidator, times(1)).isCreateUserRequestValid(any(CreateUserRequest.class), any(Locale.class));
        verify(userServiceValidator, times(1)).isPasswordValid(anyString());
        verify(userRepository, times(0)).save(any(User.class));

        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
        assertNotNull(response.getErrorMessages());
        assertTrue(response.getErrorMessages().contains("Invalid password"));
    }

    @Test
    public void create_shouldReturnFalseAnd400IfUserWithPhoneNumberAlreadyExists() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);
        when(userServiceValidator.isCreateUserRequestValid(any(CreateUserRequest.class), any(Locale.class))).thenReturn(validatorDto);
        when(userServiceValidator.isPasswordValid(anyString())).thenReturn(true);
        when(userRepository.findByPhoneNumberAndEntityStatusNot(anyString(), any(EntityStatus.class))).thenReturn(Optional.of(user));
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("User already exists");

        UserResponse response = userService.create(createUserRequest, locale, username);

        verify(userServiceValidator, times(1)).isCreateUserRequestValid(any(CreateUserRequest.class), any(Locale.class));
        verify(userServiceValidator, times(1)).isPasswordValid(anyString());
        verify(userRepository, times(1)).findByPhoneNumberAndEntityStatusNot(anyString(), any(EntityStatus.class));
        verify(userRepository, times(0)).save(any(User.class));

        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
    }

    @Test
    public void create_shouldReturnFalseAnd400IfUserWithEmailAlreadyExists() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);
        when(userServiceValidator.isCreateUserRequestValid(any(CreateUserRequest.class), any(Locale.class))).thenReturn(validatorDto);
        when(userServiceValidator.isPasswordValid(anyString())).thenReturn(true);
        when(userRepository.findByPhoneNumberAndEntityStatusNot(anyString(), any(EntityStatus.class))).thenReturn(Optional.empty());
        when(userRepository.findByEmailAndEntityStatusNot(anyString(), any(EntityStatus.class))).thenReturn(Optional.of(user));
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("User already exists");

        UserResponse response = userService.create(createUserRequest, locale, username);

        verify(userServiceValidator, times(1)).isCreateUserRequestValid(any(CreateUserRequest.class), any(Locale.class));
        verify(userServiceValidator, times(1)).isPasswordValid(anyString());
        verify(userRepository, times(1)).findByPhoneNumberAndEntityStatusNot(anyString(), any(EntityStatus.class));
        verify(userRepository, times(1)).findByEmailAndEntityStatusNot(anyString(), any(EntityStatus.class));
        verify(userRepository, times(0)).save(any(User.class));

        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
    }

    @Test
    public void findById_shouldReturnFalseAnd400IfIdIsInvalid() {
        // Create a ValidatorDto with success=false
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(false, null, List.of("Invalid ID"));
        when(userServiceValidator.isIdValid(anyLong(), any(Locale.class))).thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Invalid ID");

        UserResponse response = userService.findById(userId, locale, username);

        verify(userServiceValidator, times(1)).isIdValid(anyLong(), any(Locale.class));
        verify(userRepository, times(0)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));

        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
    }

    @Test
    public void findById_shouldReturnFalseAnd404IfUserNotFound() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);
        when(userServiceValidator.isIdValid(anyLong(), any(Locale.class))).thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class))).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("User not found");

        UserResponse response = userService.findById(userId, locale, username);

        verify(userServiceValidator, times(1)).isIdValid(anyLong(), any(Locale.class));
        verify(userRepository, times(1)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));

        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
    }

    @Test
    public void findById_shouldReturnTrueAnd200ForValidId() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);
        when(userServiceValidator.isIdValid(anyLong(), any(Locale.class))).thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class))).thenReturn(Optional.of(user));
        when(modelMapper.map(any(User.class), eq(UserDto.class))).thenReturn(userDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("User found");

        UserResponse response = userService.findById(userId, locale, username);

        verify(userServiceValidator, times(1)).isIdValid(anyLong(), any(Locale.class));
        verify(userRepository, times(1)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(modelMapper, times(1)).map(any(User.class), eq(UserDto.class));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertNotNull(response.getUserDto());
        assertEquals(userId, response.getUserDto().getId());
    }

    @Test
    public void findAllAsList_shouldReturnFalseAnd404IfNoUsersFound() {
        when(userRepository.findByEntityStatusNot(any(EntityStatus.class))).thenReturn(new ArrayList<>());
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("No users found");

        UserResponse response = userService.findAllAsList(locale, username);

        verify(userRepository, times(1)).findByEntityStatusNot(any(EntityStatus.class));

        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
    }

    @Test
    public void findAllAsList_shouldReturnTrueAnd200WithUsersList() {
        List<User> users = new ArrayList<>();
        users.add(user);
        List<UserDto> userDtos = new ArrayList<>();
        userDtos.add(userDto);

        when(userRepository.findByEntityStatusNot(any(EntityStatus.class))).thenReturn(users);
        when(modelMapper.map(anyList(), eq(new TypeToken<List<UserDto>>() {}.getType()))).thenReturn(userDtos);
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Users found");

        UserResponse response = userService.findAllAsList(locale, username);

        verify(userRepository, times(1)).findByEntityStatusNot(any(EntityStatus.class));
        verify(modelMapper, times(1)).map(anyList(), eq(new TypeToken<List<UserDto>>() {}.getType()));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertNotNull(response.getUserDtoList());
        assertEquals(1, response.getUserDtoList().size());
    }

    @Test
    public void update_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Create a ValidatorDto with success=false
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(false, null, List.of("Invalid request"));
        when(userServiceValidator.isRequestValidForEditing(any(EditUserRequest.class), any(Locale.class))).thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Invalid request");

        UserResponse response = userService.update(editUserRequest, username, locale);

        verify(userServiceValidator, times(1)).isRequestValidForEditing(any(EditUserRequest.class), any(Locale.class));
        verify(userRepository, times(0)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userServiceAuditable, times(0)).update(any(User.class), any(Locale.class), anyString());

        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
    }

    @Test
    public void update_shouldReturnFalseAnd400IfUserNotFound() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);
        when(userServiceValidator.isRequestValidForEditing(any(EditUserRequest.class), any(Locale.class))).thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class))).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("User not found");

        UserResponse response = userService.update(editUserRequest, username, locale);

        verify(userServiceValidator, times(1)).isRequestValidForEditing(any(EditUserRequest.class), any(Locale.class));
        verify(userRepository, times(1)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userServiceAuditable, times(0)).update(any(User.class), any(Locale.class), anyString());

        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
        assertNull(response.getUserDto());
    }

    @Test
    public void delete_shouldReturnFalseAnd400IfIdIsInvalid() {
        // Create a ValidatorDto with success=false
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(false, null, List.of("Invalid ID"));
        when(userServiceValidator.isIdValid(anyLong(), any(Locale.class))).thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Invalid ID");

        UserResponse response = userService.delete(userId, locale, username);

        verify(userServiceValidator, times(1)).isIdValid(anyLong(), any(Locale.class));
        verify(userRepository, times(0)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userServiceAuditable, times(0)).delete(any(User.class), any(Locale.class));

        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
    }

    @Test
    public void delete_shouldReturnFalseAnd404IfUserNotFound() {
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);
        when(userServiceValidator.isIdValid(anyLong(), any(Locale.class))).thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class))).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("User not found");

        UserResponse response = userService.delete(userId, locale, username);

        verify(userServiceValidator, times(1)).isIdValid(anyLong(), any(Locale.class));
        verify(userRepository, times(1)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userServiceAuditable, times(0)).delete(any(User.class), any(Locale.class));

        assertEquals(404, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
    }

    @Test
    public void delete_shouldReturnTrueAnd200ForValidRequest() {
        // Ensure all relationships are properly set up
        user.setUserPassword(userPassword);
        user.setUserSecurity(userSecurity);
        user.setUserPreferences(userPreferences);

        // Mock repository responses using test data from setUp()
        // Create a ValidatorDto with success=true
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(true, null, null);
        when(userServiceValidator.isIdValid(anyLong(), any(Locale.class))).thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class))).thenReturn(Optional.of(user));
        when(userAccountRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
            .thenReturn(Optional.of(user.getUserAccount()));
        when(userPasswordRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
            .thenReturn(Optional.of(userPassword));
        when(userPreferencesRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
            .thenReturn(Optional.of(userPreferences));
        when(userSecurityRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
            .thenReturn(Optional.of(userSecurity));

        // Mock service responses
        when(userAccountServiceAuditable.delete(any(UserAccount.class), any(Locale.class), anyString()))
            .thenReturn(user.getUserAccount());
        when(userPasswordServiceAuditable.delete(any(UserPassword.class), any(Locale.class)))
            .thenReturn(userPassword);
        when(userPreferencesServiceAuditable.delete(any(UserPreferences.class), any(Locale.class)))
            .thenReturn(userPreferences);
        when(userSecurityServiceAuditable.delete(any(UserSecurity.class), any(Locale.class)))
            .thenReturn(userSecurity);
        when(userServiceAuditable.delete(any(User.class), any(Locale.class)))
            .thenReturn(user);

        when(messageService.getMessage(eq(I18Code.MESSAGE_USER_DELETED_SUCCESSFULLY.getCode()), any(), any(Locale.class)))
            .thenReturn("User deleted successfully");

        when(modelMapper.map(any(User.class), eq(UserDto.class))).thenReturn(userDto);

        // Execute
        UserResponse response = userService.delete(userId, locale, username);

        // Verify validations and repository calls
        verify(userServiceValidator, times(1)).isIdValid(userId, locale);
        verify(userRepository, times(1)).findByIdAndEntityStatusNot(userId, EntityStatus.DELETED);

        // Verify related entities are retrieved for deletion
        verify(userAccountRepository, times(1)).findByIdAndEntityStatusNot(
            user.getUserAccount().getId(), EntityStatus.DELETED);
        verify(userPasswordRepository, times(1)).findByIdAndEntityStatusNot(
            userPassword.getId(), EntityStatus.DELETED);
        verify(userPreferencesRepository, times(1)).findByIdAndEntityStatusNot(
            userPreferences.getId(), EntityStatus.DELETED);
        verify(userSecurityRepository, times(1)).findByIdAndEntityStatusNot(
            userSecurity.getId(), EntityStatus.DELETED);

        // Verify all entities are deleted through their respective services
        verify(userAccountServiceAuditable, times(1)).delete(user.getUserAccount(), locale, username);
        verify(userPasswordServiceAuditable, times(1)).delete(userPassword, locale);
        verify(userPreferencesServiceAuditable, times(1)).delete(userPreferences, locale);
        verify(userSecurityServiceAuditable, times(1)).delete(userSecurity, locale);
        verify(userServiceAuditable, times(1)).delete(user, locale);

        // Verify response
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertSame(userDto, response.getUserDto());
        assertEquals("User deleted successfully", response.getMessage());
    }

    @Test
    public void findByMultipleFilters_shouldReturnFalseAnd400IfRequestIsInvalid() {
        // Create a ValidatorDto with success=false
        projectlx.co.zw.shared_library.utils.dtos.ValidatorDto validatorDto = new projectlx.co.zw.shared_library.utils.dtos.ValidatorDto(false, null, List.of("Invalid request"));
        when(userServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(any(UsersMultipleFiltersRequest.class), any(Locale.class))).thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class))).thenReturn("Invalid request");

        UserResponse response = userService.findByMultipleFilters(usersMultipleFiltersRequest, username, locale);

        verify(userServiceValidator, times(1)).isRequestValidToRetrieveUsersByMultipleFilters(any(UsersMultipleFiltersRequest.class), any(Locale.class));
        verify(userRepository, times(0)).findAll(any(Specification.class), any(Pageable.class));

        assertEquals(400, response.getStatusCode());
        assertFalse(response.isSuccess());
        assertNotNull(response);
    }

    @Test
    public void exportToCsv_shouldReturnByteArrayWithCorrectData() {
        // Prepare test data
        List<UserDto> userDtoList = new ArrayList<>();
        userDtoList.add(userDto);

        // Execute
        byte[] result = userService.exportToCsv(userDtoList);

        // Verify
        assertNotNull(result);
        assertTrue(result.length > 0);

        String csvContent = new String(result, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csvContent.contains("ID,USERNAME,EMAIL,FIRST NAME,LAST NAME,GENDER,PHONE NUMBER,NATIONAL ID,PASSPORT NUMBER,DATE OF BIRTH"));
        assertTrue(csvContent.contains(userDto.getId().toString()));
        assertTrue(csvContent.contains(userDto.getEmail()));
        assertTrue(csvContent.contains(userDto.getFirstName()));
        assertTrue(csvContent.contains(userDto.getLastName()));
    }

    @Test
    public void exportToExcel_shouldReturnByteArrayWithCorrectData() throws IOException {
        // Prepare test data
        List<UserDto> userDtoList = new ArrayList<>();
        userDtoList.add(userDto);

        // Execute
        byte[] result = userService.exportToExcel(userDtoList);

        // Verify
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void exportToPdf_shouldReturnByteArrayWithCorrectData() throws DocumentException {
        // Prepare test data
        List<UserDto> userDtoList = new ArrayList<>();
        userDtoList.add(userDto);

        // Execute
        byte[] result = userService.exportToPdf(userDtoList);

        // Verify
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void testImportUsersFromCsv_AllSuccess() throws IOException {
        // Prepare test data - CSV content with header and one user record
        String csvContent = "USERNAME,EMAIL,FIRST NAME,LAST NAME,PHONE NUMBER,NATIONAL ID,PASSPORT NUMBER,DATE OF BIRTH,GENDER\n" +
                "johndoe,john.doe@example.com,John,Doe,+1234567890,ID123456,PP123456,1990-01-01,MALE";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        // Mock the create method to return success
        UserResponse successResponse = new UserResponse();
        successResponse.setSuccess(true);
        successResponse.setStatusCode(200);
        successResponse.setMessage("User created successfully");
        successResponse.setUserDto(userDto);

        // Create a spy of the userService to mock the create method
        UserService userServiceSpy = spy(userService);
        doReturn(successResponse).when(userServiceSpy).create(any(CreateUserRequest.class), any(Locale.class), anyString());

        // Execute
        ImportSummary result = userServiceSpy.importUsersFromCsv(inputStream);

        // Verify
        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
        assertTrue(result.isSuccess());
        assertEquals(1, result.total);
        assertEquals(1, result.success);
        assertEquals(0, result.failed);
    }

    @Test
    public void testImportUsersFromCsv_PartialSuccess() throws IOException {
        // Prepare test data - CSV content with header and two user records
        String csvContent = "USERNAME,EMAIL,FIRST NAME,LAST NAME,PHONE NUMBER,NATIONAL ID,PASSPORT NUMBER,DATE OF BIRTH,GENDER\n" +
                "johndoe,john.doe@example.com,John,Doe,+1234567890,ID123456,PP123456,1990-01-01,MALE\n" +
                "janedoe,jane.doe@example.com,Jane,Doe,+0987654321,ID654321,PP654321,1992-02-02,FEMALE";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        // Mock the create method to return success for first user and failure for second user
        UserResponse successResponse = new UserResponse();
        successResponse.setSuccess(true);
        successResponse.setStatusCode(200);
        successResponse.setMessage("User created successfully");
        successResponse.setUserDto(userDto);

        UserResponse failureResponse = new UserResponse();
        failureResponse.setSuccess(false);
        failureResponse.setStatusCode(400);
        failureResponse.setMessage("User creation failed");

        // Create a spy of the userService to mock the create method
        UserService userServiceSpy = spy(userService);
        doReturn(successResponse, failureResponse).when(userServiceSpy).create(any(CreateUserRequest.class), any(Locale.class), anyString());

        // Execute
        ImportSummary result = userServiceSpy.importUsersFromCsv(inputStream);

        // Verify
        assertNotNull(result);
        assertEquals(200, result.getStatusCode());
        assertTrue(result.isSuccess());
        assertEquals(2, result.total);
        assertEquals(1, result.success);
        assertEquals(1, result.failed);
    }

    @Test
    public void testImportUsersFromCsv_AllFail() throws IOException {
        // Prepare test data - CSV content with header and one user record
        String csvContent = "USERNAME,EMAIL,FIRST NAME,LAST NAME,PHONE NUMBER,NATIONAL ID,PASSPORT NUMBER,DATE OF BIRTH,GENDER\n" +
                "johndoe,john.doe@example.com,John,Doe,+1234567890,ID123456,PP123456,1990-01-01,MALE";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        // Mock the create method to return failure
        UserResponse failureResponse = new UserResponse();
        failureResponse.setSuccess(false);
        failureResponse.setStatusCode(400);
        failureResponse.setMessage("User creation failed");

        // Create a spy of the userService to mock the create method
        UserService userServiceSpy = spy(userService);
        doReturn(failureResponse).when(userServiceSpy).create(any(CreateUserRequest.class), any(Locale.class), anyString());

        // Execute
        ImportSummary result = userServiceSpy.importUsersFromCsv(inputStream);

        // Verify
        assertNotNull(result);
        assertEquals(400, result.getStatusCode());
        assertFalse(result.isSuccess());
        assertEquals(1, result.total);
        assertEquals(0, result.success);
        assertEquals(1, result.failed);
    }
}
