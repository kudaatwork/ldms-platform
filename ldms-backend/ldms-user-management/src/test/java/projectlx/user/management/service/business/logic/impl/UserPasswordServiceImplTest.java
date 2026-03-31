package projectlx.user.management.service.business.logic.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.config.Configuration;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.auditable.api.UserPasswordServiceAuditable;
import projectlx.user.management.service.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.service.business.validator.api.UserPasswordServiceValidator;
import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.User;
import projectlx.user.management.service.model.UserPassword;
import projectlx.user.management.service.repository.UserPasswordRepository;
import projectlx.user.management.service.repository.UserRepository;
import projectlx.user.management.service.utils.dtos.UserPasswordDto;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.ChangeUserPasswordRequest;
import projectlx.user.management.service.utils.requests.CreateUserPasswordRequest;
import projectlx.user.management.service.utils.responses.UserPasswordResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

class UserPasswordServiceImplTest {

    private MessageService messageService;
    private ModelMapper modelMapper;
    private Configuration configurationMock;
    private UserPasswordRepository userPasswordRepository;
    private UserRepository userRepository;
    private UserPasswordServiceValidator userPasswordServiceValidator;
    private UserPasswordServiceAuditable userPasswordServiceAuditable;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    private UserServiceAuditable userServiceAuditable;
    private RabbitTemplate rabbitTemplate;

    private UserPasswordServiceImpl userPasswordService;

    private final Locale locale = Locale.ENGLISH;
    private final String username = "SYSTEM";

    private CreateUserPasswordRequest createUserPasswordRequest;
    private ChangeUserPasswordRequest changeUserPasswordRequest;

    private User user;
    private UserPassword userPassword;
    private UserPasswordDto userPasswordDto;
    private List<UserPassword> userPasswordList;
    private List<UserPasswordDto> userPasswordDtoList;
    private Long userPasswordId;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        messageService = mock(MessageService.class);
        modelMapper = mock(ModelMapper.class);
        configurationMock = mock(Configuration.class);
        userPasswordRepository = mock(UserPasswordRepository.class);
        userRepository = mock(UserRepository.class);
        userPasswordServiceValidator = mock(UserPasswordServiceValidator.class);
        userPasswordServiceAuditable = mock(UserPasswordServiceAuditable.class);
        bCryptPasswordEncoder = mock(BCryptPasswordEncoder.class);
        userServiceAuditable = mock(UserServiceAuditable.class);
        rabbitTemplate = mock(RabbitTemplate.class);

        // Instantiate service under test
        userPasswordService = new UserPasswordServiceImpl(
                messageService,
                modelMapper,
                userPasswordRepository,
                userRepository,
                userPasswordServiceValidator,
                userPasswordServiceAuditable,
                bCryptPasswordEncoder,
                userServiceAuditable,
                rabbitTemplate
        );

        // Stub ModelMapper configuration
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);

        // Prepare test data
        user = new User();
        user.setId(100L);
        user.setFirstName("Test");
        user.setUsername("test.user");
        user.setEmail("test.user@example.com");
        user.setPhoneNumber("+263771234567");
        user.setEntityStatus(EntityStatus.ACTIVE);

        userPasswordId = 1L;
        userPassword = new UserPassword();
        userPassword.setId(userPasswordId);
        userPassword.setUser(user);
        userPassword.setPassword("encodedPassword");
        userPassword.setExpiryDate(LocalDateTime.now().plusDays(90));
        userPassword.setIsPasswordExpired(false);
        userPassword.setEntityStatus(EntityStatus.ACTIVE);

        userPasswordDto = new UserPasswordDto();
        userPasswordDto.setId(userPasswordId);
        userPasswordDto.setExpiryDate(userPassword.getExpiryDate());
        userPasswordDto.setIsPasswordExpired(false);

        userPasswordList = new ArrayList<>();
        userPasswordList.add(userPassword);

        userPasswordDtoList = new ArrayList<>();
        userPasswordDtoList.add(userPasswordDto);

        createUserPasswordRequest = new CreateUserPasswordRequest();
        createUserPasswordRequest.setPassword("plaintextPassword");
        createUserPasswordRequest.setUserId(user.getId());

        changeUserPasswordRequest = new ChangeUserPasswordRequest();
        changeUserPasswordRequest.setId(userPasswordId);
        changeUserPasswordRequest.setPassword("newPlaintext");
        changeUserPasswordRequest.setOldPassword("oldPlaintext");
        changeUserPasswordRequest.setUserId(user.getId());
    }

    @Test
    public void create_shouldReturnFalseAnd400IfRequestIsInvalid() {
        ValidatorDto validatorDto = new ValidatorDto(false, null, Collections.singletonList("Invalid request"));
        when(userPasswordServiceValidator.isCreateUserRequestValid(any(CreateUserPasswordRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_CREATE_USER_PASSWORD_INVALID_REQUEST.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid request");

        UserPasswordResponse response = userPasswordService.create(createUserPasswordRequest, locale, username);

        verify(userPasswordServiceValidator, times(1))
                .isCreateUserRequestValid(any(CreateUserPasswordRequest.class), eq(locale));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_CREATE_USER_PASSWORD_INVALID_REQUEST.getCode()), any(String[].class), eq(locale));
        verify(userRepository, times(0)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPasswordServiceAuditable, times(0)).create(any(UserPassword.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void create_shouldReturnFalseAnd404IfUserNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userPasswordServiceValidator.isCreateUserRequestValid(any(CreateUserPasswordRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("User not found");

        UserPasswordResponse response = userPasswordService.create(createUserPasswordRequest, locale, username);

        verify(userPasswordServiceValidator, times(1))
                .isCreateUserRequestValid(any(CreateUserPasswordRequest.class), eq(locale));
        verify(userRepository, times(1))
                .findByIdAndEntityStatusNot(createUserPasswordRequest.getUserId(), EntityStatus.DELETED);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_NOT_FOUND.getCode()), any(String[].class), eq(locale));
        verify(userPasswordServiceAuditable, times(0)).create(any(UserPassword.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void create_shouldReturnTrueAnd201ForValidRequest() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userPasswordServiceValidator.isCreateUserRequestValid(any(CreateUserPasswordRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.encode(anyString())).thenReturn("encodedPwd");
        when(userPasswordServiceAuditable.create(any(UserPassword.class), any(Locale.class), anyString()))
                .thenReturn(userPassword);
        when(modelMapper.map(any(CreateUserPasswordRequest.class), eq(UserPassword.class)))
                .thenReturn(new UserPassword());
        when(modelMapper.map(any(UserPassword.class), eq(UserPasswordDto.class)))
                .thenReturn(userPasswordDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_PASSWORD_CREATED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Created");

        UserPasswordResponse response = userPasswordService.create(createUserPasswordRequest, locale, username);

        verify(userPasswordServiceValidator, times(1))
                .isCreateUserRequestValid(any(CreateUserPasswordRequest.class), eq(locale));
        verify(userRepository, times(1))
                .findByIdAndEntityStatusNot(createUserPasswordRequest.getUserId(), EntityStatus.DELETED);
        verify(modelMapper, times(1)).getConfiguration();
        verify(configurationMock, times(1)).setMatchingStrategy(MatchingStrategies.STRICT);
        verify(bCryptPasswordEncoder, times(1)).encode(createUserPasswordRequest.getPassword());
        verify(userPasswordServiceAuditable, times(1))
                .create(any(UserPassword.class), eq(locale), eq(username));
        verify(modelMapper, times(1))
                .map(userPassword, UserPasswordDto.class);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_PASSWORD_CREATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getUserPasswordDto());
    }

    @Test
    public void findById_shouldReturnFalseAnd400IfIdIsInvalid() {
        ValidatorDto validatorDto = new ValidatorDto(false, null, Collections.singletonList("Invalid ID"));
        when(userPasswordServiceValidator.isIdValid(anyLong(), eq(locale)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid ID");

        UserPasswordResponse response = userPasswordService.findById(userPasswordId, locale);

        verify(userPasswordServiceValidator, times(1)).isIdValid(userPasswordId, locale);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()), any(String[].class), eq(locale));
        verify(userPasswordRepository, times(0)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void findById_shouldReturnFalseAnd404IfNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userPasswordServiceValidator.isIdValid(anyLong(), eq(locale)))
                .thenReturn(validatorDto);
        when(userPasswordRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Not found");

        UserPasswordResponse response = userPasswordService.findById(userPasswordId, locale);

        verify(userPasswordServiceValidator, times(1)).isIdValid(userPasswordId, locale);
        verify(userPasswordRepository, times(1))
                .findByIdAndEntityStatusNot(userPasswordId, EntityStatus.DELETED);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findById_shouldReturnTrueAnd200ForValidId() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userPasswordServiceValidator.isIdValid(anyLong(), eq(locale)))
                .thenReturn(validatorDto);
        when(userPasswordRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userPassword));
        when(modelMapper.map(any(UserPassword.class), eq(UserPasswordDto.class)))
                .thenReturn(userPasswordDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_PASSWORD_RETRIEVED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Retrieved");

        UserPasswordResponse response = userPasswordService.findById(userPasswordId, locale);

        verify(userPasswordServiceValidator, times(1)).isIdValid(userPasswordId, locale);
        verify(userPasswordRepository, times(1))
                .findByIdAndEntityStatusNot(userPasswordId, EntityStatus.DELETED);
        verify(modelMapper, times(1))
                .map(userPassword, UserPasswordDto.class);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_PASSWORD_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserPasswordDto());
    }

    @Test
    public void findAllAsList_shouldReturnFalseAnd404IfEmpty() {
        when(userPasswordRepository.findByEntityStatusNot(any(EntityStatus.class)))
                .thenReturn(Collections.emptyList());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_PASSWORD_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("No passwords");

        UserPasswordResponse response = userPasswordService.findAllAsList(username, locale);

        verify(userPasswordRepository, times(1))
                .findByEntityStatusNot(EntityStatus.DELETED);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_PASSWORD_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findAllAsList_shouldReturnTrueAnd200WithList() {
        when(userPasswordRepository.findByEntityStatusNot(any(EntityStatus.class)))
                .thenReturn(userPasswordList);
        when(modelMapper.map(eq(userPasswordList), eq(new TypeToken<List<UserPasswordDto>>() {}.getType())))
                .thenReturn(userPasswordDtoList);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_PASSWORD_RETRIEVED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("List retrieved");

        UserPasswordResponse response = userPasswordService.findAllAsList(username, locale);

        verify(userPasswordRepository, times(1))
                .findByEntityStatusNot(EntityStatus.DELETED);
        verify(modelMapper, times(1))
                .map(userPasswordList, new TypeToken<List<UserPasswordDto>>() {}.getType());
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_PASSWORD_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserPasswordDtoList());
        assertFalse(response.getUserPasswordDtoList().isEmpty());
        assertEquals(1, response.getUserPasswordDtoList().size());
    }

    @Test
    public void update_shouldReturnFalseAnd400IfRequestIsInvalid() {
        ValidatorDto validatorDto = new ValidatorDto(false, null, Collections.singletonList("Invalid update"));
        when(userPasswordServiceValidator.isRequestValidForEditing(any(ChangeUserPasswordRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_UPDATE_USER_PASSWORD_INVALID_REQUEST.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid update");

        UserPasswordResponse response = userPasswordService.update(changeUserPasswordRequest, username, locale);

        verify(userPasswordServiceValidator, times(1))
                .isRequestValidForEditing(any(ChangeUserPasswordRequest.class), eq(locale));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_UPDATE_USER_PASSWORD_INVALID_REQUEST.getCode()), any(String[].class), eq(locale));
        verify(userPasswordRepository, times(0)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPasswordServiceAuditable, times(0)).update(any(UserPassword.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void update_shouldReturnFalseAnd400IfNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userPasswordServiceValidator.isRequestValidForEditing(any(ChangeUserPasswordRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        when(userPasswordRepository.findByUserIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_PASSWORD_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Not found");

        UserPasswordResponse response = userPasswordService.update(changeUserPasswordRequest, username, locale);

        verify(userPasswordServiceValidator, times(1))
                .isRequestValidForEditing(any(ChangeUserPasswordRequest.class), eq(locale));
        verify(userPasswordRepository, times(1))
                .findByUserIdAndEntityStatusNot(changeUserPasswordRequest.getUserId(), EntityStatus.DELETED);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_PASSWORD_NOT_FOUND.getCode()), any(String[].class), eq(locale));
        verify(userPasswordServiceAuditable, times(0)).update(any(UserPassword.class), any(Locale.class), anyString());
        verify(userRepository, times(0)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void update_shouldReturnTrueAnd201ForValidRequest() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userPasswordServiceValidator.isRequestValidForEditing(any(ChangeUserPasswordRequest.class), eq(locale)))
                .thenReturn(validatorDto);
        when(userPasswordRepository.findByUserIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userPassword));
        when(bCryptPasswordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(bCryptPasswordEncoder.encode(anyString())).thenReturn("encodedPwd");
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(user));
        when(userPasswordServiceAuditable.update(any(UserPassword.class), any(Locale.class), anyString()))
                .thenReturn(userPassword);
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);
        when(configurationMock.setMatchingStrategy(any())).thenReturn(configurationMock);
        when(modelMapper.map(any(UserPassword.class), eq(UserPasswordDto.class)))
                .thenReturn(userPasswordDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_PASSWORD_UPDATED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Updated");

        UserPasswordResponse response = userPasswordService.update(changeUserPasswordRequest, username, locale);

        verify(userPasswordServiceValidator, times(1))
                .isRequestValidForEditing(any(ChangeUserPasswordRequest.class), eq(locale));
        verify(userPasswordRepository, times(1))
                .findByUserIdAndEntityStatusNot(changeUserPasswordRequest.getUserId(), EntityStatus.DELETED);
        verify(userRepository, times(1))
                .findByIdAndEntityStatusNot(changeUserPasswordRequest.getUserId(), EntityStatus.DELETED);
        verify(userPasswordServiceAuditable, times(1))
                .update(any(UserPassword.class), eq(locale), eq(username));
        verify(modelMapper, times(1))
                .getConfiguration();
        verify(configurationMock, times(1)).setMatchingStrategy(MatchingStrategies.STRICT);
        verify(modelMapper, times(1))
                .map(userPassword, UserPasswordDto.class);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_PASSWORD_UPDATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserPasswordDto());
    }

    @Test
    public void delete_shouldReturnFalseAnd400IfIdIsInvalid() {
        ValidatorDto validatorDto = new ValidatorDto(false, null, Collections.singletonList("Invalid ID"));
        when(userPasswordServiceValidator.isIdValid(anyLong(), eq(locale)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid ID");

        UserPasswordResponse response = userPasswordService.delete(userPasswordId, locale);

        verify(userPasswordServiceValidator, times(1)).isIdValid(userPasswordId, locale);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()), any(String[].class), eq(locale));
        verify(userPasswordRepository, times(0)).findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userPasswordServiceAuditable, times(0)).delete(any(UserPassword.class), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void delete_shouldReturnFalseAnd404IfNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userPasswordServiceValidator.isIdValid(anyLong(), eq(locale)))
                .thenReturn(validatorDto);
        when(userPasswordRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_PASSWORD_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Not found");

        UserPasswordResponse response = userPasswordService.delete(userPasswordId, locale);

        verify(userPasswordServiceValidator, times(1)).isIdValid(userPasswordId, locale);
        verify(userPasswordRepository, times(1))
                .findByIdAndEntityStatusNot(userPasswordId, EntityStatus.DELETED);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_PASSWORD_NOT_FOUND.getCode()), any(String[].class), eq(locale));
        verify(userPasswordServiceAuditable, times(0)).delete(any(UserPassword.class), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void delete_shouldReturnTrueAnd200ForValidRequest() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userPasswordServiceValidator.isIdValid(anyLong(), eq(locale)))
                .thenReturn(validatorDto);
        when(userPasswordRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userPassword));
        when(userPasswordServiceAuditable.delete(any(UserPassword.class), any(Locale.class)))
                .thenReturn(userPassword);
        when(modelMapper.map(any(UserPassword.class), eq(UserPasswordDto.class)))
                .thenReturn(userPasswordDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_DELETED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Deleted");

        UserPasswordResponse response = userPasswordService.delete(userPasswordId, locale);

        verify(userPasswordServiceValidator, times(1)).isIdValid(userPasswordId, locale);
        verify(userPasswordRepository, times(1))
                .findByIdAndEntityStatusNot(userPasswordId, EntityStatus.DELETED);
        verify(userPasswordServiceAuditable, times(1))
                .delete(any(UserPassword.class), eq(locale));
        verify(modelMapper, times(1))
                .map(userPassword, UserPasswordDto.class);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_DELETED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserPasswordDto());
    }
}
