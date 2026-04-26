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
import projectlx.user.management.business.auditable.api.UserAccountServiceAuditable;
import projectlx.user.management.business.logic.api.UserAccountService;
import projectlx.user.management.business.validator.api.UserAccountServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserAccount;
import projectlx.user.management.repository.UserAccountRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.utils.dtos.UserAccountDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.utils.requests.EditUserAccountRequest;
import projectlx.user.management.utils.requests.UserAccountMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserAccountResponse;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountServiceImplTest {

    private UserAccountService userAccountService;
    private UserAccountServiceValidator userAccountServiceValidator;
    private UserAccountRepository userAccountRepository;
    private UserRepository userRepository;
    private UserAccountServiceAuditable userAccountServiceAuditable;
    private MessageService messageService;
    private ModelMapper modelMapper;
    private Configuration configurationMock;

    private final Locale locale = Locale.ENGLISH;
    private final String username = "SYSTEM";

    private CreateUserAccountRequest createUserAccountRequest;
    private EditUserAccountRequest editUserAccountRequest;
    private UserAccountMultipleFiltersRequest userAccountMultipleFiltersRequest;
    private UserAccount userAccount;
    private UserAccountDto userAccountDto;
    private List<UserAccount> userAccounts;
    private List<UserAccountDto> userAccountDtos;
    private User user;
    private Long userAccountId;
    private Page<UserAccount> userAccountPage;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        userAccountServiceValidator = mock(UserAccountServiceValidator.class);
        userAccountRepository = mock(UserAccountRepository.class);
        userRepository = mock(UserRepository.class);
        userAccountServiceAuditable = mock(UserAccountServiceAuditable.class);
        messageService = mock(MessageService.class);
        modelMapper = mock(ModelMapper.class);
        configurationMock = mock(Configuration.class);

        // Initialize service with mocks
        userAccountService = new UserAccountServiceImpl(
                modelMapper,
                messageService,
                userAccountRepository,
                userRepository,
                userAccountServiceAuditable,
                userAccountServiceValidator
        );

        // Common mock setup for ModelMapper configuration
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);

        // Initialize test data
        userAccountId = 1L;

        user = new User();
        user.setId(1L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEntityStatus(EntityStatus.ACTIVE);

        userAccount = new UserAccount();
        userAccount.setId(userAccountId);
        userAccount.setAccountNumber("123456789012");
        userAccount.setPhoneNumber("263771234567");
        userAccount.setIsAccountLocked(false);
        userAccount.setUser(user);
        userAccount.setEntityStatus(EntityStatus.ACTIVE);
        userAccount.setCreatedAt(LocalDateTime.now());
        userAccount.setUpdatedAt(LocalDateTime.now());

        userAccounts = new ArrayList<>();
        userAccounts.add(userAccount);

        userAccountDto = new UserAccountDto();
        userAccountDto.setId(userAccountId);
        userAccountDto.setAccountNumber("123456789012");
        userAccountDto.setPhoneNumber("263771234567");
        userAccountDto.setIsAccountLocked(false);
        userAccountDto.setEntityStatus(EntityStatus.ACTIVE);
        userAccountDto.setCreatedAt(LocalDateTime.now());
        userAccountDto.setUpdatedAt(LocalDateTime.now());

        userAccountDtos = new ArrayList<>();
        userAccountDtos.add(userAccountDto);

        createUserAccountRequest = new CreateUserAccountRequest();
        createUserAccountRequest.setUserId(1L);
        createUserAccountRequest.setPhoneNumber("263771234567");
        createUserAccountRequest.setIsAccountLocked(false);

        editUserAccountRequest = new EditUserAccountRequest();
        editUserAccountRequest.setId(userAccountId);
        editUserAccountRequest.setPhoneNumber("263771234567");
        editUserAccountRequest.setIsAccountLocked(true);

        userAccountMultipleFiltersRequest = new UserAccountMultipleFiltersRequest();
        userAccountMultipleFiltersRequest.setPage(0);
        userAccountMultipleFiltersRequest.setSize(10);

        // Initialize page data
        List<UserAccount> singleList = Collections.singletonList(userAccount);
        Pageable pageable = PageRequest.of(0, 10);
        userAccountPage = new PageImpl<>(singleList, pageable, singleList.size());
    }

    @Test
    public void create_shouldReturnFalseAnd400IfRequestIsInvalid() {
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid request");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);

        when(userAccountServiceValidator.isCreateUserRequestValid(any(CreateUserAccountRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Invalid request");

        UserAccountResponse response = userAccountService.create(createUserAccountRequest, locale, username);

        verify(userAccountServiceValidator, times(1))
                .isCreateUserRequestValid(any(CreateUserAccountRequest.class), any(Locale.class));
        verify(userRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userAccountServiceAuditable, times(0))
                .create(any(UserAccount.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void create_shouldReturnFalseAnd404IfUserNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAccountServiceValidator.isCreateUserRequestValid(any(CreateUserAccountRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("User not found");

        UserAccountResponse response = userAccountService.create(createUserAccountRequest, locale, username);

        verify(userAccountServiceValidator, times(1))
                .isCreateUserRequestValid(any(CreateUserAccountRequest.class), any(Locale.class));
        verify(userRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userAccountServiceAuditable, times(0))
                .create(any(UserAccount.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void create_shouldReturnFalseAnd400IfPhoneNumberExists() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAccountServiceValidator.isCreateUserRequestValid(any(CreateUserAccountRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userAccountRepository.findByPhoneNumberAndEntityStatusNot(
                anyString(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userAccount));
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Phone number already exists");

        UserAccountResponse response = userAccountService.create(createUserAccountRequest, locale, username);

        verify(userAccountServiceValidator, times(1))
                .isCreateUserRequestValid(any(CreateUserAccountRequest.class), any(Locale.class));
        verify(userAccountRepository, times(1))
                .findByPhoneNumberAndEntityStatusNot(anyString(), any(EntityStatus.class));
        verify(userAccountServiceAuditable, times(0))
                .create(any(UserAccount.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void create_shouldReturnTrueAnd201ForValidRequest() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAccountServiceValidator.isCreateUserRequestValid(any(CreateUserAccountRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userAccountRepository.findByPhoneNumberAndEntityStatusNot(
                eq(createUserAccountRequest.getPhoneNumber()), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());
        when(userRepository.findByIdAndEntityStatusNot(
                eq(createUserAccountRequest.getUserId()), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(user));

        // Stub ModelMapper configuration chain
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);
        when(configurationMock.setMatchingStrategy(any())).thenReturn(configurationMock);

        // Stub ModelMapper mappings
        when(modelMapper.map(any(CreateUserAccountRequest.class), eq(UserAccount.class)))
                .thenReturn(userAccount);
        when(modelMapper.map(any(UserAccount.class), eq(UserAccountDto.class)))
                .thenReturn(userAccountDto);

        when(userAccountServiceAuditable.create(any(UserAccount.class), any(Locale.class), anyString()))
                .thenReturn(userAccount);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Success message");

        UserAccountResponse response = userAccountService.create(createUserAccountRequest, locale, username);

        verify(userAccountServiceValidator, times(1))
                .isCreateUserRequestValid(createUserAccountRequest, locale);
        verify(userAccountRepository, times(1))
                .findByPhoneNumberAndEntityStatusNot(createUserAccountRequest.getPhoneNumber(), EntityStatus.DELETED);
        verify(userRepository, times(1))
                .findByIdAndEntityStatusNot(createUserAccountRequest.getUserId(), EntityStatus.DELETED);
        verify(modelMapper, times(1))
                .map(createUserAccountRequest, UserAccount.class);
        verify(userAccountServiceAuditable, times(1))
                .create(any(UserAccount.class), any(Locale.class), anyString());
        verify(modelMapper, times(1))
                .map(userAccount, UserAccountDto.class);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getUserAccountDto());
    }

    @Test
    public void findById_shouldReturnFalseAnd400IfIdIsInvalid() {
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid ID");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);

        when(userAccountServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Invalid ID");

        UserAccountResponse response = userAccountService.findById(userAccountId, locale, username);

        verify(userAccountServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userAccountRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void findById_shouldReturnFalseAnd404IfUserAccountNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAccountServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userAccountRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("User account not found");

        UserAccountResponse response = userAccountService.findById(userAccountId, locale, username);

        verify(userAccountServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userAccountRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findById_shouldReturnTrueAnd200ForValidId() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAccountServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userAccountRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userAccount));
        when(modelMapper.map(any(UserAccount.class), eq(UserAccountDto.class)))
                .thenReturn(userAccountDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Success message");

        UserAccountResponse response = userAccountService.findById(userAccountId, locale, username);

        verify(userAccountServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userAccountRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(modelMapper, times(1))
                .map(userAccount, UserAccountDto.class);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserAccountDto());
    }

    @Test
    public void findAllAsList_shouldReturnFalseAnd404IfNoUserAccountsFound() {
        when(userAccountRepository.findByEntityStatusNot(any(EntityStatus.class)))
                .thenReturn(Collections.emptyList());
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("No user accounts found");

        UserAccountResponse response = userAccountService.findAllAsList(username, locale);

        verify(userAccountRepository, times(1))
                .findByEntityStatusNot(EntityStatus.DELETED);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findAllAsList_shouldReturnTrueAnd200WithUserAccountsList() {
        // 1) Use the same TypeToken as the implementation (List<UserAccount>),
        //    even though it returns List<UserAccountDto>.
        Type listType = new TypeToken<List<UserAccount>>() {}.getType();

        // 2) Stub repository to return a non‐empty list
        when(userAccountRepository.findByEntityStatusNot(EntityStatus.DELETED))
                .thenReturn(userAccounts);

        // 3) Stub ModelMapper to return our DTO list when given that same TypeToken
        when(modelMapper.map(eq(userAccounts), eq(listType)))
                .thenReturn(userAccountDtos);

        // 4) Stub the “retrieved successfully” message using the exact I18 code
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ACCOUNT_RETRIEVED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Users retrieved successfully");

        // Act
        UserAccountResponse response = userAccountService.findAllAsList(username, locale);

        // Verify that findByEntityStatusNot was called once with DELETED
        verify(userAccountRepository, times(1))
                .findByEntityStatusNot(EntityStatus.DELETED);

        // Verify that ModelMapper.map was called with the same listType
        verify(modelMapper, times(1))
                .map(userAccounts, listType);

        // Verify that messageService.getMessage was called with the correct I18 code and locale
        verify(messageService, times(1))
                .getMessage(
                        I18Code.MESSAGE_USER_ACCOUNT_RETRIEVED_SUCCESSFULLY.getCode(),
                        new String[]{},
                        locale
                );

        // Assertions
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());

        assertNotNull(response.getUserAccountDtoList());
        assertFalse(response.getUserAccountDtoList().isEmpty());
        assertEquals(1, response.getUserAccountDtoList().size());
    }

    @Test
    public void update_shouldReturnFalseAnd400IfRequestIsInvalid() {
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid request");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);

        when(userAccountServiceValidator.isRequestValidForEditing(any(EditUserAccountRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Invalid request");

        UserAccountResponse response = userAccountService.update(editUserAccountRequest, username, locale);

        verify(userAccountServiceValidator, times(1))
                .isRequestValidForEditing(any(EditUserAccountRequest.class), any(Locale.class));
        verify(userAccountRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userAccountServiceAuditable, times(0))
                .update(any(UserAccount.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void update_shouldReturnFalseAnd400IfUserAccountNotFound() {
        // 1) Validator returns true
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAccountServiceValidator.isRequestValidForEditing(any(EditUserAccountRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);

        // 2) Stub repository to return empty
        when(userAccountRepository.findByIdAndEntityStatusNot(
                eq(editUserAccountRequest.getId()), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.empty());

        // 3) Stub message for “not found”
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ACCOUNT_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("User account not found");

        // Act
        UserAccountResponse response = userAccountService.update(editUserAccountRequest, username, locale);

        // Verify validator and repository calls
        verify(userAccountServiceValidator, times(1))
                .isRequestValidForEditing(eq(editUserAccountRequest), any(Locale.class));
        verify(userAccountRepository, times(1))
                .findByIdAndEntityStatusNot(eq(editUserAccountRequest.getId()), eq(EntityStatus.DELETED));

        // Ensure no call to the auditable update
        verify(userAccountServiceAuditable, times(0))
                .update(any(UserAccount.class), any(Locale.class), anyString());

        // Verify getMessage with the correct I18 code
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ACCOUNT_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        // Assert
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void update_shouldReturnTrueAnd200ForValidRequest() {
        // 1) Validator says the request is valid:
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAccountServiceValidator.isRequestValidForEditing(any(EditUserAccountRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);

        // 2) Stub findByIdAndEntityStatusNot(...) to return our existing UserAccount
        when(userAccountRepository.findByIdAndEntityStatusNot(
                eq(editUserAccountRequest.getId()), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userAccount));

        // 3) Stub the auditable update(...) call
        when(userAccountServiceAuditable.update(any(UserAccount.class), any(Locale.class), anyString()))
                .thenReturn(userAccount);

        // 4) Stub ModelMapper configuration and mapping
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);
        when(configurationMock.setMatchingStrategy(any())).thenReturn(configurationMock);
        when(modelMapper.map(any(UserAccount.class), eq(UserAccountDto.class)))
                .thenReturn(userAccountDto);

        // 5) Stub the “updated successfully” message
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Update successful");

        // Act
        UserAccountResponse response = userAccountService.update(editUserAccountRequest, username, locale);

        // Verify only the calls that actually happen in update(...)
        verify(userAccountServiceValidator, times(1))
                .isRequestValidForEditing(editUserAccountRequest, locale);
        verify(userAccountRepository, times(1))
                .findByIdAndEntityStatusNot(editUserAccountRequest.getId(), EntityStatus.DELETED);
        verify(userAccountServiceAuditable, times(1))
                .update(any(UserAccount.class), any(Locale.class), anyString());
        verify(modelMapper, times(1))
                .map(userAccount, UserAccountDto.class);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserAccountDto());
    }

    @Test
    public void delete_shouldReturnFalseAnd400IfIdIsInvalid() {
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid ID");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);

        when(userAccountServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Invalid ID");

        UserAccountResponse response = userAccountService.delete(userAccountId, locale, username);

        verify(userAccountServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userAccountRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userAccountServiceAuditable, times(0))
                .delete(any(UserAccount.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void delete_shouldReturnFalseAnd404IfUserAccountNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAccountServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userAccountRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("User account not found");

        UserAccountResponse response = userAccountService.delete(userAccountId, locale, username);

        verify(userAccountServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userAccountRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userAccountServiceAuditable, times(0))
                .delete(any(UserAccount.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void delete_shouldReturnTrueAnd200ForValidRequest() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);

        when(userAccountServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userAccountRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userAccount));
        when(userAccountServiceAuditable.delete(any(UserAccount.class), any(Locale.class), anyString()))
                .thenReturn(userAccount);
        when(modelMapper.map(any(UserAccount.class), eq(UserAccountDto.class)))
                .thenReturn(userAccountDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Delete successful");

        UserAccountResponse response = userAccountService.delete(userAccountId, locale, username);

        verify(userAccountServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userAccountRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userAccountServiceAuditable, times(1))
                .delete(any(UserAccount.class), any(Locale.class), anyString());
        verify(modelMapper, times(1))
                .map(userAccount, UserAccountDto.class);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserAccountDto());
    }

    @Test
    public void findByMultipleFilters_shouldReturnFalseAnd400IfRequestIsInvalid() {
        List<String> errorMessages = new ArrayList<>();
        errorMessages.add("Invalid request");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errorMessages);

        when(userAccountServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Invalid request");

        UserAccountResponse response = userAccountService.findByMultipleFilters(
                userAccountMultipleFiltersRequest, username, locale
        );

        verify(userAccountServiceValidator, times(1))
                .isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class));
        verify(userAccountRepository, times(0))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void findByMultipleFilters_shouldReturnFalseAnd404IfNoUserAccountsFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        ValidatorDto invalidStringValidatorDto = new ValidatorDto(false, null, Collections.singletonList("Invalid string"));

        when(userAccountServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class)))
                .thenReturn(validatorDto);
        // Handle null values for string fields
        when(userAccountServiceValidator.isStringValid(any(), any(Locale.class)))
                .thenReturn(invalidStringValidatorDto);
        when(userAccountServiceValidator.isBooleanValid(any(), any(Locale.class)))
                .thenReturn(invalidStringValidatorDto);
        when(userAccountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("No user accounts found");

        UserAccountResponse response = userAccountService.findByMultipleFilters(
                userAccountMultipleFiltersRequest, username, locale
        );

        verify(userAccountServiceValidator, times(1))
                .isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class));
        verify(userAccountRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findByMultipleFilters_shouldReturnTrueAnd200WithUserAccountsPage() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        ValidatorDto invalidStringValidatorDto = new ValidatorDto(false, null, Collections.singletonList("Invalid string"));

        when(userAccountServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class)))
                .thenReturn(validatorDto);
        // Handle both null and non-null string inputs
        when(userAccountServiceValidator.isStringValid(any(), any(Locale.class)))
                .thenReturn(invalidStringValidatorDto);
        when(userAccountServiceValidator.isBooleanValid(any(), any(Locale.class)))
                .thenReturn(invalidStringValidatorDto);
        when(userAccountRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(userAccountPage);
        when(modelMapper.map(any(UserAccount.class), eq(UserAccountDto.class)))
                .thenReturn(userAccountDto);
        when(messageService.getMessage(anyString(), any(), any(Locale.class)))
                .thenReturn("Success message");

        UserAccountResponse response = userAccountService.findByMultipleFilters(
                userAccountMultipleFiltersRequest, username, locale
        );

        verify(userAccountServiceValidator, times(1))
                .isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class));
        verify(userAccountRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(modelMapper, times(1))
                .map(userAccount, UserAccountDto.class);
        verify(messageService, times(1))
                .getMessage(anyString(), any(), any(Locale.class));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserAccountDtoPage());
        assertFalse(response.getUserAccountDtoPage().isEmpty());
        assertEquals(1, response.getUserAccountDtoPage().getTotalElements());
    }

    @Test
    public void exportToCsv_shouldReturnByteArray() {
        List<UserAccountDto> dtos = Collections.singletonList(userAccountDto);

        byte[] result = userAccountService.exportToCsv(dtos);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void exportToExcel_shouldReturnByteArray() throws Exception {
        List<UserAccountDto> dtos = Collections.singletonList(userAccountDto);

        byte[] result = userAccountService.exportToExcel(dtos);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void exportToPdf_shouldReturnByteArray() throws Exception {
        List<UserAccountDto> dtos = Collections.singletonList(userAccountDto);

        byte[] result = userAccountService.exportToPdf(dtos);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
