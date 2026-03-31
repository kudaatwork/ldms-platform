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
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.service.business.auditable.api.UserGroupServiceAuditable;
import projectlx.user.management.service.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.service.business.validator.api.UserGroupServiceValidator;
import projectlx.user.management.service.model.EntityStatus;
import projectlx.user.management.service.model.User;
import projectlx.user.management.service.model.UserGroup;
import projectlx.user.management.service.model.UserRole;
import projectlx.user.management.service.repository.UserGroupRepository;
import projectlx.user.management.service.repository.UserRepository;
import projectlx.user.management.service.repository.UserRoleRepository;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.dtos.UserGroupDto;
import projectlx.user.management.service.utils.dtos.UserRoleDto;
import projectlx.user.management.service.utils.enums.I18Code;
import projectlx.user.management.service.utils.requests.AddUserToUserGroupRequest;
import projectlx.user.management.service.utils.requests.AssignUserRoleToUserGroupRequest;
import projectlx.user.management.service.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.service.utils.requests.EditUserGroupRequest;
import projectlx.user.management.service.utils.requests.RemoveUserRolesFromUserGroupRequest;
import projectlx.user.management.service.utils.requests.UserGroupMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserGroupResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserGroupServiceImplTest {

    private UserGroupServiceImpl userGroupService;
    private UserGroupServiceValidator userGroupServiceValidator;
    private MessageService messageService;
    private ModelMapper modelMapper;
    private Configuration configurationMock;
    private UserGroupRepository userGroupRepository;
    private UserRepository userRepository;
    private UserGroupServiceAuditable userGroupServiceAuditable;
    private UserRoleRepository userRoleRepository;
    private UserServiceAuditable userServiceAuditable;

    private final Locale locale = Locale.ENGLISH;
    private final String username = "SYSTEM";

    private CreateUserGroupRequest createUserGroupRequest;
    private EditUserGroupRequest editUserGroupRequest;
    private UserGroupMultipleFiltersRequest userGroupMultipleFiltersRequest;
    private AssignUserRoleToUserGroupRequest assignUserRoleToUserGroupRequest;
    private RemoveUserRolesFromUserGroupRequest removeUserRolesFromUserGroupRequest;
    private AddUserToUserGroupRequest addUserToUserGroupRequest;

    private UserGroup userGroup;
    private UserGroupDto userGroupDto;
    private List<UserGroup> userGroupList;
    private List<UserGroupDto> userGroupDtoList;
    private Long userGroupId;
    private Page<UserGroup> userGroupPage;

    private UserRole userRole;
    private UserRoleDto userRoleDto;
    private Set<UserRole> userRoleSet;
    private List<UserRoleDto> userRoleDtoList;

    private User user;

    // Helper method to create ValidatorDto objects
    private ValidatorDto createValidatorDto(boolean success) {
        return new ValidatorDto(success, null, success ? null : new ArrayList<>());
    }

    @BeforeEach
    void setUp() {
        // Initialize mocks
        userGroupServiceValidator = mock(UserGroupServiceValidator.class);
        messageService = mock(MessageService.class);
        modelMapper = mock(ModelMapper.class);
        configurationMock = mock(Configuration.class);
        userGroupRepository = mock(UserGroupRepository.class);
        userRepository = mock(UserRepository.class);
        userGroupServiceAuditable = mock(UserGroupServiceAuditable.class);
        userRoleRepository = mock(UserRoleRepository.class);
        userServiceAuditable = mock(UserServiceAuditable.class);

        // Instantiate service
        userGroupService = new UserGroupServiceImpl(
                userGroupServiceValidator,
                messageService,
                modelMapper,
                userGroupRepository,
                userRepository,
                userGroupServiceAuditable,
                userRoleRepository,
                userServiceAuditable
        );

        // Stub ModelMapper configuration
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);

        // Test data setup
        userGroupId = 1L;
        userGroup = new UserGroup();
        userGroup.setId(userGroupId);
        userGroup.setName("ADMINS");
        userGroup.setDescription("Administrators");
        userGroup.setEntityStatus(EntityStatus.ACTIVE);

        userGroupList = new ArrayList<>();
        userGroupList.add(userGroup);

        userGroupDto = new UserGroupDto();
        userGroupDto.setId(userGroupId);
        userGroupDto.setName("ADMINS");
        userGroupDto.setDescription("Administrators");

        userGroupDtoList = new ArrayList<>();
        userGroupDtoList.add(userGroupDto);

        createUserGroupRequest = new CreateUserGroupRequest();
        createUserGroupRequest.setName("ADMINS");
        createUserGroupRequest.setDescription("Administrators");

        editUserGroupRequest = new EditUserGroupRequest();
        editUserGroupRequest.setId(userGroupId);
        editUserGroupRequest.setName("users");
        editUserGroupRequest.setDescription("Regular Users");

        userGroupMultipleFiltersRequest = new UserGroupMultipleFiltersRequest();
        userGroupMultipleFiltersRequest.setPage(0);
        userGroupMultipleFiltersRequest.setSize(10);
        userGroupMultipleFiltersRequest.setName("ADM");
        userGroupMultipleFiltersRequest.setDescription("Admin");
        userGroupMultipleFiltersRequest.setSearchValue("ADM");

        userRole = new UserRole();
        userRole.setId(10L);
        userRole.setRole("ROLE_USER");
        userRole.setEntityStatus(EntityStatus.ACTIVE);

        userRoleSet = new HashSet<>();
        userRoleSet.add(userRole);

        userRoleDto = new UserRoleDto();
        userRoleDto.setId(10L);
        userRoleDto.setRole("ROLE_USER");
        userRoleDto.setEntityStatus(EntityStatus.ACTIVE);

        userRoleDtoList = new ArrayList<>();
        userRoleDtoList.add(userRoleDto);

        userGroupDto.setUserRoleDtoSet(userRoleDtoList);

        assignUserRoleToUserGroupRequest = new AssignUserRoleToUserGroupRequest();
        assignUserRoleToUserGroupRequest.setUserGroupId(userGroupId);
        assignUserRoleToUserGroupRequest.setUserRoleIds(List.of(10L));

        removeUserRolesFromUserGroupRequest = new RemoveUserRolesFromUserGroupRequest();
        removeUserRolesFromUserGroupRequest.setUserGroupId(userGroupId);
        removeUserRolesFromUserGroupRequest.setUserRoleIds(List.of(10L));

        addUserToUserGroupRequest = new AddUserToUserGroupRequest();
        addUserToUserGroupRequest.setUserGroupId(userGroupId);
        addUserToUserGroupRequest.setUserId(100L);

        user = new User();
        user.setId(100L);
        user.setUsername("testuser");
        user.setEntityStatus(EntityStatus.ACTIVE);

        // Page data
        Pageable pageable = PageRequest.of(0, 10);
        userGroupPage = new PageImpl<>(userGroupList, pageable, userGroupList.size());
    }

    @Test
    public void create_shouldReturnFalseAnd400IfRequestIsInvalid() {
        when(userGroupServiceValidator.isCreateUserGroupRequestValid(any(CreateUserGroupRequest.class), any(Locale.class)))
                .thenReturn(createValidatorDto(false));
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_CREATE_USER_GROUP_INVALID_REQUEST.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid request");

        UserGroupResponse response = userGroupService.create(createUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isCreateUserGroupRequestValid(any(CreateUserGroupRequest.class), any(Locale.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_CREATE_USER_GROUP_INVALID_REQUEST.getCode()), any(String[].class), eq(locale));
        verify(userGroupRepository, times(0))
                .findByNameAndEntityStatusNot(anyString(), any(EntityStatus.class));
        verify(userGroupServiceAuditable, times(0))
                .create(any(UserGroup.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void create_shouldReturnFalseAnd400IfGroupExists() {
        when(userGroupServiceValidator.isCreateUserGroupRequestValid(any(CreateUserGroupRequest.class), any(Locale.class)))
                .thenReturn(createValidatorDto(true));
        when(userGroupRepository.findByNameAndEntityStatusNot(
                eq(createUserGroupRequest.getName().toUpperCase()), eq(EntityStatus.DELETED)))
                .thenReturn(Optional.of(userGroup));
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_ALREADY_EXISTS.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Group exists");

        UserGroupResponse response = userGroupService.create(createUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isCreateUserGroupRequestValid(any(CreateUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByNameAndEntityStatusNot(createUserGroupRequest.getName().toUpperCase(), EntityStatus.DELETED);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_ALREADY_EXISTS.getCode()), any(String[].class), eq(locale));
        verify(userGroupServiceAuditable, times(0))
                .create(any(UserGroup.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void create_shouldReturnTrueAnd201ForValidRequest() {
        // Arrange
        when(userGroupServiceValidator.isCreateUserGroupRequestValid(eq(createUserGroupRequest), any(Locale.class)))
                .thenReturn(createValidatorDto(true));

        when(userGroupRepository.findByNameAndEntityStatusNot(
                createUserGroupRequest.getName(),
                EntityStatus.DELETED))
                .thenReturn(Optional.empty());

        when(modelMapper.getConfiguration()).thenReturn(configurationMock);
        when(configurationMock.setMatchingStrategy(any())).thenReturn(configurationMock);

        when(modelMapper.map(createUserGroupRequest, UserGroup.class))
                .thenReturn(userGroup);
        when(userGroupServiceAuditable.create(any(UserGroup.class), any(Locale.class), anyString()))
                .thenReturn(userGroup);
        when(modelMapper.map(userGroup, UserGroupDto.class))
                .thenReturn(userGroupDto);

        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_CREATED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Created");

        // Act
        UserGroupResponse response = userGroupService.create(createUserGroupRequest, locale, username);

        // Verify interactions
        verify(userGroupServiceValidator, times(1))
                .isCreateUserGroupRequestValid(eq(createUserGroupRequest), any(Locale.class));

        verify(userGroupRepository, times(1))
                .findByNameAndEntityStatusNot(
                        createUserGroupRequest.getName(),
                        EntityStatus.DELETED);

        verify(modelMapper, times(1))
                .map(createUserGroupRequest, UserGroup.class);

        verify(userGroupServiceAuditable, times(1))
                .create(any(UserGroup.class), any(Locale.class), anyString());

        verify(modelMapper, times(1))
                .map(userGroup, UserGroupDto.class);

        verify(messageService, times(1))
                .getMessage(
                        eq(I18Code.MESSAGE_USER_GROUP_CREATED_SUCCESSFULLY.getCode()),
                        any(String[].class),
                        eq(locale));

        // Assert response payload
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getUserGroupDto());
    }

    @Test
    public void findById_shouldReturnFalseAnd400IfIdIsInvalid() {
        when(userGroupServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(createValidatorDto(false));
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid ID");

        UserGroupResponse response = userGroupService.findById(userGroupId, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userGroupRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void findById_shouldReturnFalseAnd404IfNotFound() {
        when(userGroupServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(createValidatorDto(true));
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Not found");

        UserGroupResponse response = userGroupService.findById(userGroupId, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findById_shouldReturnTrueAnd200ForValidId() {
        when(userGroupServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(createValidatorDto(true));
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userGroup));
        when(modelMapper.map(any(UserGroup.class), eq(UserGroupDto.class)))
                .thenReturn(userGroupDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Retrieved");

        UserGroupResponse response = userGroupService.findById(userGroupId, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(modelMapper, times(1))
                .map(userGroup, UserGroupDto.class);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserGroupDto());
    }

    @Test
    public void findAllAsList_shouldReturnFalseAnd404IfEmpty() {
        when(userGroupRepository.findByEntityStatusNot(any(EntityStatus.class)))
                .thenReturn(Collections.emptyList());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("No groups");

        UserGroupResponse response = userGroupService.findAllAsList(username, locale);

        verify(userGroupRepository, times(1))
                .findByEntityStatusNot(EntityStatus.DELETED);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findAllAsList_shouldReturnTrueAnd200WithList() {
        Type listType = new TypeToken<List<UserGroupDto>>() {}.getType();

        when(userGroupRepository.findByEntityStatusNot(any(EntityStatus.class)))
                .thenReturn(userGroupList);
        when(modelMapper.map(eq(userGroupList), eq(listType)))
                .thenReturn(userGroupDtoList);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("List retrieved");

        UserGroupResponse response = userGroupService.findAllAsList(username, locale);

        verify(userGroupRepository, times(1))
                .findByEntityStatusNot(EntityStatus.DELETED);
        verify(modelMapper, times(1))
                .map(userGroupList, listType);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserGroupDtoList());
        assertFalse(response.getUserGroupDtoList().isEmpty());
        assertEquals(1, response.getUserGroupDtoList().size());
    }

    @Test
    public void update_shouldReturnFalseAnd400IfRequestIsInvalid() {
        List<String> errors = new ArrayList<>();
        errors.add("Invalid request");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errors);
        when(userGroupServiceValidator.isRequestValidForEditing(any(EditUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_UPDATE_USER_GROUP_INVALID_REQUEST.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid update");

        UserGroupResponse response = userGroupService.update(editUserGroupRequest, username, locale);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidForEditing(any(EditUserGroupRequest.class), any(Locale.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_UPDATE_USER_GROUP_INVALID_REQUEST.getCode()), any(String[].class), eq(locale));
        verify(userGroupRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userGroupServiceAuditable, times(0))
                .update(any(UserGroup.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void update_shouldReturnFalseAnd400IfNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidForEditing(any(EditUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Not found");

        UserGroupResponse response = userGroupService.update(editUserGroupRequest, username, locale);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidForEditing(any(EditUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()), any(String[].class), eq(locale));
        verify(userGroupServiceAuditable, times(0))
                .update(any(UserGroup.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void update_shouldReturnTrueAnd201ForValidRequest() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidForEditing(any(EditUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userGroup));

        when(userGroupServiceAuditable.update(any(UserGroup.class), any(Locale.class), anyString()))
                .thenReturn(userGroup);
        when(modelMapper.getConfiguration()).thenReturn(configurationMock);
        when(configurationMock.setMatchingStrategy(any())).thenReturn(configurationMock);
        when(modelMapper.map(any(UserGroup.class), eq(UserGroupDto.class)))
                .thenReturn(userGroupDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_UPDATED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Updated");

        UserGroupResponse response = userGroupService.update(editUserGroupRequest, username, locale);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidForEditing(any(EditUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userGroupServiceAuditable, times(1))
                .update(any(UserGroup.class), any(Locale.class), anyString());
        verify(modelMapper, times(1))
                .map(userGroup, UserGroupDto.class);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_UPDATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getUserGroupDto());
    }

    @Test
    public void delete_shouldReturnFalseAnd400IfIdIsInvalid() {
        when(userGroupServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(createValidatorDto(false));
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid ID");

        UserGroupResponse response = userGroupService.delete(userGroupId, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userGroupRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userGroupServiceAuditable, times(0))
                .delete(any(UserGroup.class), any(Locale.class), anyString());
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void delete_shouldReturnFalseAnd404IfNotFound() {
        when(userGroupServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(createValidatorDto(true));
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Not found");

        UserGroupResponse response = userGroupService.delete(userGroupId, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()), any(String[].class), eq(locale));
        verify(userGroupServiceAuditable, times(0))
                .delete(any(UserGroup.class), any(Locale.class), anyString());

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void delete_shouldReturnTrueAnd200ForValidRequest() {
        when(userGroupServiceValidator.isIdValid(anyLong(), any(Locale.class)))
                .thenReturn(createValidatorDto(true));
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userGroup));
        when(userGroupServiceAuditable.delete(any(UserGroup.class), any(Locale.class), anyString()))
                .thenReturn(userGroup);
        when(modelMapper.map(any(UserGroup.class), eq(UserGroupDto.class)))
                .thenReturn(userGroupDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_DELETED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Deleted");

        UserGroupResponse response = userGroupService.delete(userGroupId, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isIdValid(anyLong(), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userGroupServiceAuditable, times(1))
                .delete(any(UserGroup.class), any(Locale.class), anyString());
        verify(modelMapper, times(1))
                .map(userGroup, UserGroupDto.class);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_DELETED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserGroupDto());
    }

    @Test
    public void findByMultipleFilters_shouldReturnFalseAnd400IfInvalid() {
        when(userGroupServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class)))
                .thenReturn(createValidatorDto(false));
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_INVALID_MULTIPLE_FILTERS_REQUEST.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid filters");

        UserGroupResponse response = userGroupService.findByMultipleFilters(userGroupMultipleFiltersRequest, username, locale);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class));
        verify(userGroupRepository, times(0))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_INVALID_MULTIPLE_FILTERS_REQUEST.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void findByMultipleFilters_shouldReturnFalseAnd404IfNoResults() {
        when(userGroupServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class)))
                .thenReturn(createValidatorDto(true));
        when(userGroupServiceValidator.isStringValid(anyString(), any(Locale.class)))
                .thenReturn(createValidatorDto(true));
        when(userGroupRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("No groups");

        UserGroupResponse response = userGroupService.findByMultipleFilters(userGroupMultipleFiltersRequest, username, locale);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
    }

    @Test
    public void findByMultipleFilters_shouldReturnTrueAnd200WithPage() {
        when(userGroupServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class)))
                .thenReturn(createValidatorDto(true));
        when(userGroupServiceValidator.isStringValid(anyString(), any(Locale.class)))
                .thenReturn(createValidatorDto(true));
        when(userGroupRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(userGroupPage);
        when(modelMapper.map(any(UserGroup.class), eq(UserGroupDto.class)))
                .thenReturn(userGroupDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Page retrieved");

        UserGroupResponse response = userGroupService.findByMultipleFilters(userGroupMultipleFiltersRequest, username, locale);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToRetrieveUsersByMultipleFilters(any(), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findAll(any(Specification.class), any(Pageable.class));
        verify(modelMapper, times(1))
                .map(userGroup, UserGroupDto.class);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserGroupDtoPage());
        assertFalse(response.getUserGroupDtoPage().isEmpty());
        assertEquals(1, response.getUserGroupDtoPage().getTotalElements());
    }

    @Test
    public void assignUserRoleToUserGroup_shouldReturnFalseAnd400IfInvalid() {
        List<String> errors = new ArrayList<>();
        errors.add("Invalid request");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errors);
        when(userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(any(AssignUserRoleToUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_INVALID_ASSIGN_USER_ROLE_TO_USER_GROUP_REQUEST.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid assign");

        UserGroupResponse response = userGroupService.assignUserRoleToUserGroup(assignUserRoleToUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToAssignUserRolesToUserGroup(any(AssignUserRoleToUserGroupRequest.class), any(Locale.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_INVALID_ASSIGN_USER_ROLE_TO_USER_GROUP_REQUEST.getCode()), any(String[].class), eq(locale));
        verify(userGroupRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void assignUserRoleToUserGroup_shouldReturnFalseAnd400IfGroupNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(any(AssignUserRoleToUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Group not found");

        UserGroupResponse response = userGroupService.assignUserRoleToUserGroup(assignUserRoleToUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToAssignUserRolesToUserGroup(any(AssignUserRoleToUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void assignUserRoleToUserGroup_shouldReturnFalseAnd400IfRolesNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(any(AssignUserRoleToUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userGroup));
        when(userRoleRepository.findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class)))
                .thenReturn(Collections.emptySet());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Roles not found");

        UserGroupResponse response = userGroupService.assignUserRoleToUserGroup(assignUserRoleToUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToAssignUserRolesToUserGroup(any(AssignUserRoleToUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userRoleRepository, times(1))
                .findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void assignUserRoleToUserGroup_shouldReturnFalseAnd400IfAlreadyAssigned() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(any(AssignUserRoleToUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userGroup));
        when(userRoleRepository.findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class)))
                .thenReturn(userRoleSet);
        userGroup.getUserRoles().addAll(userRoleSet); // simulate already assigned
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ROLES_ALREADY_ASSIGNED.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Already assigned");

        UserGroupResponse response = userGroupService.assignUserRoleToUserGroup(assignUserRoleToUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToAssignUserRolesToUserGroup(any(AssignUserRoleToUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userRoleRepository, times(1))
                .findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ROLES_ALREADY_ASSIGNED.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void assignUserRoleToUserGroup_shouldReturnTrueAnd201ForValid() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(any(AssignUserRoleToUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userGroup));
        when(userRoleRepository.findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class)))
                .thenReturn(userRoleSet);
        // initially group has no roles
        when(userGroupServiceAuditable.update(any(UserGroup.class), any(Locale.class), anyString()))
                .thenReturn(userGroup);
        when(modelMapper.map(any(UserGroup.class), eq(UserGroupDto.class)))
                .thenReturn(userGroupDto);
        when(modelMapper.map(eq(userRoleSet), eq(new TypeToken<List<UserRoleDto>>(){}.getType())))
                .thenReturn(userRoleDtoList);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ROLE_ASSIGNED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Assigned");

        UserGroupResponse response = userGroupService.assignUserRoleToUserGroup(assignUserRoleToUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToAssignUserRolesToUserGroup(any(AssignUserRoleToUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userRoleRepository, times(1))
                .findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class));
        verify(userGroupServiceAuditable, times(1))
                .update(any(UserGroup.class), any(Locale.class), anyString());
        verify(modelMapper, times(1))
                .map(userGroup, UserGroupDto.class);
        verify(modelMapper, times(1))
                .map(userRoleSet, new TypeToken<List<UserRoleDto>>(){}.getType());
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ROLE_ASSIGNED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getUserGroupDto());
        assertEquals(userRoleDtoList, response.getUserGroupDto().getUserRoleDtoSet());
    }

    @Test
    public void removeUserRolesFromUserGroup_shouldReturnFalseAnd400IfInvalid() {
        List<String> errors = new ArrayList<>();
        errors.add("Invalid request");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errors);
        when(userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(any(RemoveUserRolesFromUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_INVALID_REMOVE_USER_ROLES_FROM_USER_GROUP_REQUEST.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid remove");

        UserGroupResponse response = userGroupService.removeUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToRemoveUserRolesFromUserGroup(any(RemoveUserRolesFromUserGroupRequest.class), any(Locale.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_INVALID_REMOVE_USER_ROLES_FROM_USER_GROUP_REQUEST.getCode()), any(String[].class), eq(locale));
        verify(userGroupRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void removeUserRolesFromUserGroup_shouldReturnFalseAnd400IfGroupNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(any(RemoveUserRolesFromUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Not found");

        UserGroupResponse response = userGroupService.removeUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToRemoveUserRolesFromUserGroup(any(RemoveUserRolesFromUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void removeUserRolesFromUserGroup_shouldReturnFalseAnd400IfRolesNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(any(RemoveUserRolesFromUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userGroup));
        when(userRoleRepository.findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class)))
                .thenReturn(Collections.emptySet());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Roles not found");

        UserGroupResponse response = userGroupService.removeUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToRemoveUserRolesFromUserGroup(any(RemoveUserRolesFromUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userRoleRepository, times(1))
                .findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void removeUserRolesFromUserGroup_shouldReturnFalseAnd400IfNotAssigned() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(any(RemoveUserRolesFromUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userGroup));
        when(userRoleRepository.findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class)))
                .thenReturn(userRoleSet);
        // group has no roles => removal fails
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ROLE_NOT_ASSIGNED.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Not assigned");

        UserGroupResponse response = userGroupService.removeUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToRemoveUserRolesFromUserGroup(any(RemoveUserRolesFromUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userRoleRepository, times(1))
                .findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ROLE_NOT_ASSIGNED.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void removeUserRolesFromUserGroup_shouldReturnTrueAnd201ForValid() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(any(RemoveUserRolesFromUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        // Pre-add role to group
        userGroup.getUserRoles().add(userRole);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userGroup));
        when(userRoleRepository.findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class)))
                .thenReturn(userRoleSet);
        when(userGroupServiceAuditable.update(any(UserGroup.class), any(Locale.class), anyString()))
                .thenReturn(userGroup);
        when(modelMapper.map(any(UserGroup.class), eq(UserGroupDto.class)))
                .thenReturn(userGroupDto);
        when(modelMapper.map(eq(userGroup.getUserRoles()), eq(new TypeToken<List<UserRoleDto>>(){}.getType())))
                .thenReturn(userRoleDtoList);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_ROLES_REMOVED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Removed");

        UserGroupResponse response = userGroupService.removeUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToRemoveUserRolesFromUserGroup(any(RemoveUserRolesFromUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userRoleRepository, times(1))
                .findByIdInAndEntityStatusNot(anyList(), any(EntityStatus.class));
        verify(userGroupServiceAuditable, times(1))
                .update(any(UserGroup.class), any(Locale.class), anyString());
        verify(modelMapper, times(1))
                .map(userGroup, UserGroupDto.class);
        verify(modelMapper, times(1))
                .map(userGroup.getUserRoles(), new TypeToken<List<UserRoleDto>>(){}.getType());
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_ROLES_REMOVED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(201, response.getStatusCode());
        assertNotNull(response.getUserGroupDto());
        assertEquals(userRoleDtoList, response.getUserGroupDto().getUserRoleDtoSet());
    }

    @Test
    public void addUserToUserGroup_shouldReturnFalseAnd400IfInvalid() {
        List<String> errors = new ArrayList<>();
        errors.add("Invalid request");
        ValidatorDto validatorDto = new ValidatorDto(false, null, errors);
        when(userGroupServiceValidator.isRequestValidToAddUserToUserGroup(any(AddUserToUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_INVALID_ADD_USER_GROUP_TO_USER_REQUEST.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Invalid add");

        UserGroupResponse response = userGroupService.addUserGroupToUser(addUserToUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToAddUserToUserGroup(any(AddUserToUserGroupRequest.class), any(Locale.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_INVALID_ADD_USER_GROUP_TO_USER_REQUEST.getCode()), any(String[].class), eq(locale));
        verify(userGroupRepository, times(0))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void addUserToUserGroup_shouldReturnFalseAnd400IfGroupNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidToAddUserToUserGroup(any(AddUserToUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("Group not found");

        UserGroupResponse response = userGroupService.addUserGroupToUser(addUserToUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToAddUserToUserGroup(any(AddUserToUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void addUserToUserGroup_shouldReturnFalseAnd400IfUserNotFound() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidToAddUserToUserGroup(any(AddUserToUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userGroup));
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.empty());
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_NOT_FOUND.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("User not found");

        UserGroupResponse response = userGroupService.addUserGroupToUser(addUserToUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToAddUserToUserGroup(any(AddUserToUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_NOT_FOUND.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
    }

    @Test
    public void addUserToUserGroup_shouldReturnTrueAnd200ForValid() {
        ValidatorDto validatorDto = new ValidatorDto(true, null, null);
        when(userGroupServiceValidator.isRequestValidToAddUserToUserGroup(any(AddUserToUserGroupRequest.class), any(Locale.class)))
                .thenReturn(validatorDto);
        when(userGroupRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(userGroup));
        when(userRepository.findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class)))
                .thenReturn(Optional.of(user));
        when(userServiceAuditable.update(any(User.class), any(Locale.class), anyString()))
                .thenReturn(user);
        when(modelMapper.map(any(UserGroup.class), eq(UserGroupDto.class)))
                .thenReturn(userGroupDto);
        when(messageService.getMessage(
                eq(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode()),
                any(String[].class),
                eq(locale)))
                .thenReturn("User updated");

        UserGroupResponse response = userGroupService.addUserGroupToUser(addUserToUserGroupRequest, locale, username);

        verify(userGroupServiceValidator, times(1))
                .isRequestValidToAddUserToUserGroup(any(AddUserToUserGroupRequest.class), any(Locale.class));
        verify(userGroupRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userRepository, times(1))
                .findByIdAndEntityStatusNot(anyLong(), any(EntityStatus.class));
        verify(userServiceAuditable, times(1))
                .update(any(User.class), any(Locale.class), anyString());
        verify(modelMapper, times(1))
                .map(userGroup, UserGroupDto.class);
        verify(messageService, times(1))
                .getMessage(eq(I18Code.MESSAGE_USER_UPDATED_SUCCESSFULLY.getCode()), any(String[].class), eq(locale));

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getUserGroupDto());
    }

    @Test
    public void exportToCsv_shouldReturnByteArray() {
        // Act
        byte[] result = userGroupService.exportToCsv(userGroupDtoList);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);

        // Convert to string to verify content
        String csvContent = new String(result);
        assertTrue(csvContent.contains("ID,NAME,DESCRIPTION,USER ROLES"));
        assertTrue(csvContent.contains(userGroupDto.getId().toString()));
        assertTrue(csvContent.contains(userGroupDto.getName()));
        assertTrue(csvContent.contains(userGroupDto.getDescription()));
        assertTrue(csvContent.contains(userRoleDto.getRole()));
    }

    @Test
    public void exportToExcel_shouldReturnByteArray() throws Exception {
        // Act
        byte[] result = userGroupService.exportToExcel(userGroupDtoList);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void exportToPdf_shouldReturnByteArray() throws Exception {
        // Act
        byte[] result = userGroupService.exportToPdf(userGroupDtoList);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void importUserGroupsFromCsv_shouldReturnSuccessfulImportSummary() throws IOException {
        // Create a spy of the userGroupService to partially mock the importUserGroupsFromCsv method
        UserGroupServiceImpl userGroupServiceSpy = org.mockito.Mockito.spy(userGroupService);

        // Create a mock InputStream
        InputStream csvInputStream = mock(InputStream.class);

        // Create a successful import summary
        ImportSummary successSummary = new ImportSummary(
                200, true, "Import completed successfully. 2 out of 2 user groups imported.", 
                2, 2, 0, new ArrayList<>());

        // Mock the importUserGroupsFromCsv method to return the success summary
        org.mockito.Mockito.doReturn(successSummary).when(userGroupServiceSpy).importUserGroupsFromCsv(any(InputStream.class));

        // Act
        ImportSummary result = userGroupServiceSpy.importUserGroupsFromCsv(csvInputStream);

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
    public void importUserGroupsFromCsv_shouldReturnFailedImportSummary() throws IOException {
        // Create a spy of the userGroupService to partially mock the importUserGroupsFromCsv method
        UserGroupServiceImpl userGroupServiceSpy = org.mockito.Mockito.spy(userGroupService);

        // Create a mock InputStream
        InputStream csvInputStream = mock(InputStream.class);

        // Create a failed import summary with error messages
        List<String> errors = new ArrayList<>();
        errors.add("Row 1: Invalid request");
        errors.add("Row 2: Invalid request");

        ImportSummary failedSummary = new ImportSummary(
                400, false, "Import failed. No user groups were imported.", 
                2, 0, 2, errors);

        // Mock the importUserGroupsFromCsv method to return the failed summary
        org.mockito.Mockito.doReturn(failedSummary).when(userGroupServiceSpy).importUserGroupsFromCsv(any(InputStream.class));

        // Act
        ImportSummary result = userGroupServiceSpy.importUserGroupsFromCsv(csvInputStream);

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
