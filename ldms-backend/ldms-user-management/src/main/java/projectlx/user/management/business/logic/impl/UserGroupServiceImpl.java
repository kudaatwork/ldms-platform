package projectlx.user.management.business.logic.impl;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.shared_library.utils.export.LdmsExportReport;
import projectlx.co.zw.shared_library.utils.export.LdmsPdfReportWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.security.AdministratorRoleScopePolicy;
import projectlx.user.management.business.auditable.api.UserGroupServiceAuditable;
import projectlx.user.management.business.auditable.api.UserServiceAuditable;
import projectlx.user.management.business.logic.support.OrganizationWorkspaceAccessSupport;
import projectlx.user.management.business.logic.support.OrganizationWorkspaceProvisioner;
import projectlx.user.management.business.logic.api.UserGroupService;
import projectlx.user.management.business.validator.api.UserGroupServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserGroup;
import projectlx.user.management.model.UserRole;
import projectlx.user.management.repository.UserGroupRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.repository.UserRoleRepository;
import projectlx.user.management.repository.specification.UserGroupSpecification;
import projectlx.user.management.utils.dtos.UserGroupDto;
import projectlx.user.management.utils.dtos.UserRoleDto;
import projectlx.user.management.utils.support.UserRoleDtoModuleEnricher;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.AddUserToUserGroupRequest;
import projectlx.user.management.utils.requests.AddUsersToUserGroupRequest;
import projectlx.user.management.utils.requests.AssignUserRoleToUserGroupRequest;
import projectlx.user.management.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.utils.requests.EditUserGroupRequest;
import projectlx.user.management.utils.requests.RemoveUserRolesFromUserGroupRequest;
import projectlx.user.management.utils.requests.RemoveUsersFromUserGroupRequest;
import projectlx.user.management.utils.requests.UserGroupMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserGroupResponse;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import projectlx.user.management.utils.dtos.ImportSummary;

@RequiredArgsConstructor
public class UserGroupServiceImpl implements UserGroupService {

    private static final Logger log = LoggerFactory.getLogger(UserGroupServiceImpl.class);

    private final UserGroupServiceValidator userGroupServiceValidator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;
    private final UserGroupServiceAuditable userGroupServiceAuditable;
    private final UserRoleRepository userRoleRepository;
    private final UserServiceAuditable userServiceAuditable;
    private final OrganizationWorkspaceAccessSupport organizationWorkspaceAccessSupport;

    private static final String[] HEADERS = {
            "ID", "NAME", "DESCRIPTION", "USER ROLES"
    };

    private static final String[] CSV_HEADERS = {
            "NAME", "DESCRIPTION"
    };

    @Override
    public UserGroupResponse create(CreateUserGroupRequest createUserGroupRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isCreateUserGroupRequestValid(createUserGroupRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_GROUP_INVALID_REQUEST.getCode(), new String[]{},
                        locale);

            return buildUserGroupResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        String normalizedName = createUserGroupRequest.getName().toUpperCase();
        Long workspaceOrganizationId = resolveOrganizationIdForWorkspaceMutation(
                username, createUserGroupRequest.getOrganizationId());

        if (OrganizationWorkspaceProvisioner.ADMINISTRATOR_GROUP_NAME.equalsIgnoreCase(normalizedName)) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_GROUP_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);
            return buildUserGroupResponseWithErrors(400, false, message, null, null,
                    List.of("The Administrator group is provisioned automatically. New roles are added to it as they are introduced."));
        }

        Optional<UserGroup> userGroupRetrieved = existingActiveGroupByName(normalizedName, workspaceOrganizationId);

        if (userGroupRetrieved.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_ALREADY_EXISTS.getCode(), new String[]{},
                        locale);

            return buildUserGroupResponse(400, false, message, null,
                        null, null);
        }

        Optional<UserGroup> deletedUserGroup = existingDeletedGroupByName(normalizedName, workspaceOrganizationId);

        createUserGroupRequest.setName(normalizedName);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserGroup userGroupSaved;
        if (deletedUserGroup.isPresent()) {
            UserGroup userGroupToBeReactivated = deletedUserGroup.get();
            userGroupToBeReactivated.setName(normalizedName);
            userGroupToBeReactivated.setDescription(createUserGroupRequest.getDescription());
            userGroupToBeReactivated.setEntityStatus(EntityStatus.ACTIVE);
            if (workspaceOrganizationId != null && workspaceOrganizationId > 0) {
                userGroupToBeReactivated.setOrganizationId(workspaceOrganizationId);
            }
            userGroupSaved = userGroupServiceAuditable.update(userGroupToBeReactivated, locale, username);
        } else {
            UserGroup userGroupToBeSaved = modelMapper.map(createUserGroupRequest, UserGroup.class);
            if (workspaceOrganizationId != null && workspaceOrganizationId > 0) {
                userGroupToBeSaved.setOrganizationId(workspaceOrganizationId);
            }
            userGroupSaved = userGroupServiceAuditable.create(userGroupToBeSaved, locale, username);
        }

        UserGroupDto useGroupDtoReturned = modelMapper.map(userGroupSaved, UserGroupDto.class);
        enrichMemberCount(useGroupDtoReturned);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                    locale);

        return buildUserGroupResponse(201, true, message, useGroupDtoReturned, null,
                    null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserGroupResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Optional<UserGroup> userGroupRetrieved = userGroupRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userGroupRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(404, false, message, null, null,
                    null);
        }

        UserGroup userGroupReturned = userGroupRetrieved.get();
        UserGroupDto userGroupDto = modelMapper.map(userGroupReturned, UserGroupDto.class);
        // DTO uses userRoleDtoSet; entity uses userRoles (ManyToMany). Map roles inside the transaction so the lazy collection loads.
        Set<UserRole> roleSet = userGroupReturned.getUserRoles();
        List<UserRoleDto> roleDtos = new ArrayList<>();
        if (roleSet != null) {
            for (UserRole role : roleSet) {
                if (role.getEntityStatus() != EntityStatus.DELETED) {
                    roleDtos.add(modelMapper.map(role, UserRoleDto.class));
                }
            }
        }
        UserRoleDtoModuleEnricher.enrichAll(roleDtos);
        roleDtos = filterRoleDtosForPortalScope(roleDtos, resolvePortalScopeForSession(username));
        userGroupDto.setUserRoleDtoSet(roleDtos);
        enrichMemberCount(userGroupDto);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserGroupResponse(200, true, message, userGroupDto, null,
                null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserGroupResponse findAllAsList(String username, Locale locale) {

        String message = "";

        List<UserGroup> userGroupList = collapseDuplicateAdministratorGroups(
                userGroupRepository.findByEntityStatusNot(EntityStatus.DELETED));

        if(userGroupList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildUserGroupResponse(404, false, message, null,
                    null, null);
        }

        List<UserGroupDto> userGroupDtoList = modelMapper.map(userGroupList, new TypeToken<List<UserGroupDto>>(){}.getType());
        for (UserGroupDto dto : userGroupDtoList) {
            enrichMemberCount(dto);
        }

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserGroupResponse(200, true, message, null, userGroupDtoList,
                null);
    }

    @Override
    public UserGroupResponse update(EditUserGroupRequest editUserGroupRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isRequestValidForEditing(editUserGroupRequest, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_GROUP_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Optional<UserGroup> userGroupRetrieved = userGroupRepository.findByIdAndEntityStatusNot(editUserGroupRequest.getId(),
                EntityStatus.DELETED);

        if (userGroupRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        UserGroup userGroupToBeEdited = userGroupRetrieved.get();

        userGroupToBeEdited.setName(editUserGroupRequest.getName());
        userGroupToBeEdited.setDescription(editUserGroupRequest.getDescription());

        UserGroup userGroupEdited = userGroupServiceAuditable.update(userGroupToBeEdited, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserGroupDto userGroupDtoReturned = modelMapper.map(userGroupEdited, UserGroupDto.class);
        enrichMemberCount(userGroupDtoReturned);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserGroupResponse(201, true, message, userGroupDtoReturned, null,
                null);
    }

    @Override
    @Transactional
    public UserGroupResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Optional<UserGroup> userGroupRetrieved = userGroupRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userGroupRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(404, false, message, null, null,
                    null);
        }

        UserGroup userGroupToBeDeleted = userGroupRetrieved.get();
        detachAllUsersFromGroup(userGroupToBeDeleted.getId(), locale, username);
        // Drop join-table links only for this group; UserRole rows stay and remain linked to other groups.
        if (userGroupToBeDeleted.getUserRoles() != null) {
            userGroupToBeDeleted.getUserRoles().clear();
        }
        userGroupToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        UserGroup userGroupDeleted = userGroupServiceAuditable.delete(userGroupToBeDeleted, locale, username);

        UserGroupDto useGroupDtoReturned = modelMapper.map(userGroupDeleted, UserGroupDto.class);
        enrichMemberCount(useGroupDtoReturned);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserGroupResponse(200, true, message, useGroupDtoReturned, null,
                null);
    }

    @Override
    public UserGroupResponse findByMultipleFilters(UserGroupMultipleFiltersRequest userGroupMultipleFiltersRequest, String username, Locale locale) {

        String message = "";

        Specification<UserGroup> spec = null;
        spec = addToSpec(spec, UserGroupSpecification::deleted);

        ValidatorDto validatorDto = userGroupServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(
                userGroupMultipleFiltersRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Pageable pageable = PageRequest.of(userGroupMultipleFiltersRequest.getPage(),
                userGroupMultipleFiltersRequest.getSize());

        ValidatorDto nameValidatorDto = userGroupServiceValidator.isStringValid(
                userGroupMultipleFiltersRequest.getName(), locale);

        if (nameValidatorDto.getSuccess()) {

            spec = addToSpec(userGroupMultipleFiltersRequest.getName(), spec,
                    UserGroupSpecification::nameLike);
        }

        ValidatorDto descriptionValidatorDto = userGroupServiceValidator.isStringValid(
                userGroupMultipleFiltersRequest.getDescription(), locale);

        if (descriptionValidatorDto.getSuccess()) {

            spec = addToSpec(userGroupMultipleFiltersRequest.getDescription(), spec,
                    UserGroupSpecification::descriptionLike);
        }

        ValidatorDto searchValueValidatorDto = userGroupServiceValidator.isStringValid(
                userGroupMultipleFiltersRequest.getSearchValue(), locale);

        if (searchValueValidatorDto.getSuccess()) {

            spec = addToSpec(userGroupMultipleFiltersRequest.getSearchValue(), spec, UserGroupSpecification::any);
        }

        Optional<Long> workspaceOrganizationId = resolveWorkspaceOrganizationFilter(
                username, userGroupMultipleFiltersRequest);
        if (workspaceOrganizationId.isPresent()) {
            spec = addToSpec(workspaceOrganizationId.get(), spec, UserGroupSpecification::organizationWorkspaceVisible);
        }

        Page<UserGroup> result = userGroupRepository.findAll(spec, pageable);
        List<UserGroup> collapsed = collapseDuplicateAdministratorGroups(result.getContent());
        Page<UserGroup> collapsedPage = new PageImpl<>(collapsed, pageable, collapsed.size());

        Page<UserGroupDto> userGroupDtoPage = convertUserGroupEntityToUserGroupDto(collapsedPage);

        message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserGroupResponse(200, true, message,null, null,
                userGroupDtoPage);
    }

    @Override
    @Transactional
    public UserGroupResponse assignUserRoleToUserGroup(AssignUserRoleToUserGroupRequest assignUserRoleToUserGroupRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isRequestValidToAssignUserRolesToUserGroup(assignUserRoleToUserGroupRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVALID_ASSIGN_USER_ROLE_TO_USER_GROUP_REQUEST.getCode(),
                    new String[]{}, locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Optional<UserGroup> userGroup =
                userGroupRepository.findByIdAndEntityStatusNot(assignUserRoleToUserGroupRequest.getUserGroupId(),
                        EntityStatus.DELETED);

        if (userGroup.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        UserGroup userGroupToBeUpdated = userGroup.get();

        Set<UserRole> userRoleSet = userRoleRepository.findByIdInAndEntityStatusNot(
                assignUserRoleToUserGroupRequest.getUserRoleIds(), EntityStatus.DELETED);

        if (userRoleSet.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        Set<Long> existingRoleIds = userGroupToBeUpdated.getUserRoles().stream()
                .map(UserRole::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<UserRole> rolesToAssign = userRoleSet.stream()
                .filter(role -> role.getId() != null && !existingRoleIds.contains(role.getId()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        int skippedRoleCount = userRoleSet.size() - rolesToAssign.size();

        if (rolesToAssign.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLES_ALREADY_ASSIGNED.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        Set<UserRole> mergedRoles = new LinkedHashSet<>(userGroupToBeUpdated.getUserRoles());
        mergedRoles.addAll(rolesToAssign);
        userGroupToBeUpdated.setUserRoles(mergedRoles);

        UserGroup userGroupUpdated = userGroupServiceAuditable.update(userGroupToBeUpdated, locale, username);

        UserGroupDto userGroupDto = modelMapper.map(userGroupUpdated, UserGroupDto.class);

        List<UserRoleDto> userRoleDtoList = modelMapper.map(userGroupUpdated.getUserRoles(),
                new TypeToken<List<UserRoleDto>>() {}.getType());
        UserRoleDtoModuleEnricher.enrichAll(userRoleDtoList);

        userGroupDto.setUserRoleDtoSet(userRoleDtoList);
        enrichMemberCount(userGroupDto);

        if (skippedRoleCount > 0) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_USER_ROLES_ASSIGNED_WITH_SKIPPED.getCode(),
                    new String[]{String.valueOf(skippedRoleCount), String.valueOf(rolesToAssign.size())},
                    locale);
        } else {
            message = messageService.getMessage(
                    I18Code.MESSAGE_USER_ROLE_ASSIGNED_SUCCESSFULLY.getCode(), new String[]{},
                    locale);
        }

        return buildUserGroupResponse(201, true, message, userGroupDto, null,
                null);
    }

    @Override
    @Transactional
    public UserGroupResponse removeUserRolesFromUserGroup(RemoveUserRolesFromUserGroupRequest removeUserRolesFromUserGroupRequest, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = userGroupServiceValidator.isRequestValidToRemoveUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVALID_REMOVE_USER_ROLES_FROM_USER_GROUP_REQUEST.getCode(),
                    new String[]{}, locale);

            UserGroupResponse response = buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
            return response;
        }

        Optional<UserGroup> userGroup =
                userGroupRepository.findByIdAndEntityStatusNot(removeUserRolesFromUserGroupRequest.getUserGroupId(),
                        EntityStatus.DELETED);

        if (userGroup.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        UserGroup userGroupToBeUpdated = userGroup.get();

        Set<UserRole> userRoleSet = userRoleRepository.findByIdInAndEntityStatusNot(
                removeUserRolesFromUserGroupRequest.getUserRoleIds(), EntityStatus.DELETED);

        if (userRoleSet.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        Set<UserRole> remainingUserRoles = userGroupToBeUpdated.getUserRoles();

        boolean removed = remainingUserRoles.removeAll(userRoleSet);

        if(!removed) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_NOT_ASSIGNED.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        userGroupToBeUpdated.setUserRoles(remainingUserRoles);

        UserGroup userGroupReturned = userGroupServiceAuditable.update(userGroupToBeUpdated, locale, username);

        UserGroupDto userGroupDto = modelMapper.map(userGroupReturned, UserGroupDto.class);

        List<UserRoleDto> userRoleDtoList = modelMapper.map(userGroupReturned.getUserRoles(),
                new TypeToken<List<UserRoleDto>>() {}.getType());
        UserRoleDtoModuleEnricher.enrichAll(userRoleDtoList);

        userGroupDto.setUserRoleDtoSet(userRoleDtoList);
        enrichMemberCount(userGroupDto);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ROLES_REMOVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserGroupResponse(201, true, message, userGroupDto,
                null, null);
    }

    @Override
    @Transactional
    public UserGroupResponse addUserGroupToUser(AddUserToUserGroupRequest addUserToUserGroupRequest, Locale locale, String username) {

        AddUsersToUserGroupRequest batchRequest = new AddUsersToUserGroupRequest();
        batchRequest.setUserGroupId(addUserToUserGroupRequest.getUserGroupId());
        batchRequest.setUserIds(List.of(addUserToUserGroupRequest.getUserId()));
        return addUsersToUserGroup(batchRequest, locale, username);
    }

    @Override
    @Transactional
    public UserGroupResponse addUsersToUserGroup(AddUsersToUserGroupRequest addUsersToUserGroupRequest, Locale locale,
                                                 String username) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isRequestValidToAddUsersToUserGroup(
                addUsersToUserGroupRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVALID_ADD_USERS_TO_USER_GROUP_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<UserGroup> userGroupOptional =
                userGroupRepository.findByIdAndEntityStatusNot(addUsersToUserGroupRequest.getUserGroupId(),
                        EntityStatus.DELETED);

        if (userGroupOptional.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        UserGroup targetGroup = userGroupOptional.get();
        Set<Long> affectedGroupIds = new LinkedHashSet<>();
        affectedGroupIds.add(targetGroup.getId());
        int skippedUserCount = 0;
        int addedUserCount = 0;

        for (Long userId : addUsersToUserGroupRequest.getUserIds()) {

            Optional<User> userOptional = userRepository.findByIdAndEntityStatusNot(userId, EntityStatus.DELETED);

            if (userOptional.isEmpty()) {

                message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(),
                        new String[]{}, locale);

                return buildUserGroupResponse(400, false, message, null, null,
                        null);
            }

            User user = userOptional.get();
            UserGroup previousGroup = user.getUserGroup();

            if (previousGroup != null
                    && previousGroup.getId() != null
                    && previousGroup.getId().equals(targetGroup.getId())) {
                skippedUserCount++;
                continue;
            }

            if (previousGroup != null && previousGroup.getId() != null) {
                affectedGroupIds.add(previousGroup.getId());
            }

            user.setUserGroup(targetGroup);
            userServiceAuditable.update(user, locale, username);
            addedUserCount++;
        }

        if (addedUserCount == 0) {

            message = messageService.getMessage(I18Code.MESSAGE_USERS_ALREADY_IN_USER_GROUP.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        List<UserGroupDto> affectedGroupDtos = buildEnrichedUserGroupDtos(affectedGroupIds);
        UserGroupDto primaryDto = affectedGroupDtos.stream()
                .filter(dto -> dto.getId() != null && dto.getId().equals(targetGroup.getId()))
                .findFirst()
                .orElse(affectedGroupDtos.isEmpty() ? null : affectedGroupDtos.get(0));

        if (skippedUserCount > 0) {
            message = messageService.getMessage(
                    I18Code.MESSAGE_USERS_ADDED_TO_USER_GROUP_WITH_SKIPPED.getCode(),
                    new String[]{String.valueOf(skippedUserCount), String.valueOf(addedUserCount)},
                    locale);
        } else {
            message = messageService.getMessage(I18Code.MESSAGE_USERS_ADDED_TO_USER_GROUP_SUCCESSFULLY.getCode(),
                    new String[]{}, locale);
        }

        return buildUserGroupResponse(200, true, message, primaryDto, affectedGroupDtos,
                null);
    }

    @Override
    @Transactional
    public UserGroupResponse removeUsersFromUserGroup(RemoveUsersFromUserGroupRequest removeUsersFromUserGroupRequest,
                                                      Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userGroupServiceValidator.isRequestValidToRemoveUsersFromUserGroup(
                removeUsersFromUserGroupRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_INVALID_REMOVE_USERS_FROM_USER_GROUP_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponseWithErrors(400, false, message, null, null, validatorDto.getErrorMessages());
        }

        Optional<UserGroup> userGroupOptional =
                userGroupRepository.findByIdAndEntityStatusNot(removeUsersFromUserGroupRequest.getUserGroupId(),
                        EntityStatus.DELETED);

        if (userGroupOptional.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_GROUP_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        UserGroup userGroup = userGroupOptional.get();
        boolean removedAny = false;

        for (Long userId : removeUsersFromUserGroupRequest.getUserIds()) {

            Optional<User> userOptional = userRepository.findByIdAndEntityStatusNot(userId, EntityStatus.DELETED);

            if (userOptional.isEmpty()) {

                message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(),
                        new String[]{}, locale);

                return buildUserGroupResponse(400, false, message, null, null,
                        null);
            }

            User user = userOptional.get();
            UserGroup assignedGroup = user.getUserGroup();

            if (assignedGroup == null || assignedGroup.getId() == null
                    || !assignedGroup.getId().equals(userGroup.getId())) {

                message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_ASSIGNED_TO_USER_GROUP.getCode(),
                        new String[]{}, locale);

                return buildUserGroupResponse(400, false, message, null, null,
                        null);
            }

            user.setUserGroup(null);
            userServiceAuditable.update(user, locale, username);
            removedAny = true;
        }

        if (!removedAny) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_ASSIGNED_TO_USER_GROUP.getCode(),
                    new String[]{}, locale);

            return buildUserGroupResponse(400, false, message, null, null,
                    null);
        }

        UserGroupDto userGroupDto = modelMapper.map(userGroup, UserGroupDto.class);
        enrichMemberCount(userGroupDto);

        message = messageService.getMessage(I18Code.MESSAGE_USERS_REMOVED_FROM_USER_GROUP_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserGroupResponse(200, true, message, userGroupDto, List.of(userGroupDto),
                null);
    }

    private List<UserGroupDto> buildEnrichedUserGroupDtos(Set<Long> groupIds) {
        List<UserGroupDto> dtoList = new ArrayList<>();
        if (groupIds == null || groupIds.isEmpty()) {
            return dtoList;
        }
        for (Long groupId : groupIds) {
            if (groupId == null || groupId < 1) {
                continue;
            }
            Optional<UserGroup> groupOptional = userGroupRepository.findByIdAndEntityStatusNot(groupId, EntityStatus.DELETED);
            if (groupOptional.isEmpty()) {
                continue;
            }
            UserGroupDto dto = modelMapper.map(groupOptional.get(), UserGroupDto.class);
            enrichMemberCount(dto);
            dtoList.add(dto);
        }
        return dtoList;
    }

    private void detachAllUsersFromGroup(Long userGroupId, Locale locale, String username) {
        if (userGroupId == null || userGroupId < 1) {
            return;
        }
        List<User> assignedUsers = userRepository.findByUserGroup_IdAndEntityStatusNot(userGroupId, EntityStatus.DELETED);
        for (User user : assignedUsers) {
            UserGroup assignedGroup = user.getUserGroup();
            if (assignedGroup == null || assignedGroup.getId() == null
                    || !assignedGroup.getId().equals(userGroupId)) {
                continue;
            }
            user.setUserGroup(null);
            userServiceAuditable.update(user, locale, username);
        }
    }

    private void enrichMemberCount(UserGroupDto dto) {
        if (dto == null || dto.getId() == null) {
            return;
        }
        try {
            dto.setUserMemberCount(userRepository.countActiveUsersForUserGroup(dto.getId(), EntityStatus.DELETED));
        } catch (Exception ex) {
            log.warn("Failed to resolve userMemberCount for userGroupId={}: {}", dto.getId(), ex.toString());
            dto.setUserMemberCount(0L);
        }
        try {
            dto.setUserRoleMemberCount(
                    userRoleRepository.countActiveRolesForUserGroup(dto.getId(), EntityStatus.DELETED));
        } catch (Exception ex) {
            log.warn("Failed to resolve userRoleMemberCount for userGroupId={}: {}", dto.getId(), ex.toString());
            dto.setUserRoleMemberCount(0L);
        }
    }

    private Optional<Long> resolveWorkspaceOrganizationFilter(
            String username, UserGroupMultipleFiltersRequest request) {
        if (organizationWorkspaceAccessSupport.hasWorkspaceSuperRole()) {
            Long requested = request.getOrganizationId();
            if (requested != null && requested > 0) {
                if (!organizationWorkspaceAccessSupport.canReadOrganization(username, requested)) {
                    return Optional.empty();
                }
                return Optional.of(requested);
            }
            return Optional.empty();
        }
        return organizationWorkspaceAccessSupport.sessionOrganizationId(username);
    }

    private Long resolveOrganizationIdForWorkspaceMutation(String username, Long requestedOrganizationId) {
        if (organizationWorkspaceAccessSupport.hasWorkspaceSuperRole()) {
            return requestedOrganizationId != null && requestedOrganizationId > 0
                    ? requestedOrganizationId
                    : null;
        }
        return organizationWorkspaceAccessSupport.sessionOrganizationId(username).orElse(null);
    }

    private Optional<UserGroup> existingActiveGroupByName(String normalizedName, Long organizationId) {
        if (organizationId != null && organizationId > 0) {
            return userGroupRepository.findByOrganizationIdAndNameIgnoreCaseAndEntityStatusNot(
                    organizationId, normalizedName, EntityStatus.DELETED);
        }
        return userGroupRepository.findByNameAndEntityStatusNot(normalizedName, EntityStatus.DELETED);
    }

    private Optional<UserGroup> existingDeletedGroupByName(String normalizedName, Long organizationId) {
        return userGroupRepository.findByName(normalizedName)
                .filter(group -> group.getEntityStatus() == EntityStatus.DELETED
                        && (organizationId == null
                        || organizationId <= 0
                        || organizationId.equals(group.getOrganizationId())));
    }

    private Specification<UserGroup> addToSpec(final Long value, Specification<UserGroup> spec,
            Function<Long, Specification<UserGroup>> predicateMethod) {
        if (value != null && value > 0) {
            Specification<UserGroup> localSpec = Specification.where(predicateMethod.apply(value));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<UserGroup> addToSpec(Specification<UserGroup> spec,
                                                 Function<EntityStatus, Specification<UserGroup>> predicateMethod) {
        Specification<UserGroup> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<UserGroup> addToSpec(final String aString, Specification<UserGroup> spec, Function<String,
            Specification<UserGroup>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<UserGroup> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

        private Page<UserGroupDto> convertUserGroupEntityToUserGroupDto(Page<UserGroup> userGroupPage) {

        List<UserGroupDto> userGroupDtoList = new ArrayList<>();

        for (UserGroup userGroup : userGroupPage) {
            UserGroupDto userGroupDto = modelMapper.map(userGroup, UserGroupDto.class);
            enrichMemberCount(userGroupDto);
            userGroupDtoList.add(userGroupDto);
        }

        int page = userGroupPage.getNumber();
        int size = userGroupPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableUserGroups = PageRequest.of(page, size);

        return new PageImpl<UserGroupDto>(userGroupDtoList, pageableUserGroups, userGroupPage.getTotalElements());
    }

    private List<UserGroup> collapseDuplicateAdministratorGroups(List<UserGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        boolean hasOrgScopedAdministrator = groups.stream()
                .anyMatch(group -> group != null
                        && isAdministratorGroupName(group.getName())
                        && group.getOrganizationId() != null
                        && group.getOrganizationId() > 0);
        List<UserGroup> filtered = new ArrayList<>();
        boolean platformAdministratorAdded = false;
        for (UserGroup group : groups) {
            if (group == null) {
                continue;
            }
            if (isAdministratorGroupName(group.getName())) {
                Long organizationId = group.getOrganizationId();
                if (organizationId == null || organizationId <= 0) {
                    if (hasOrgScopedAdministrator) {
                        continue;
                    }
                    if (platformAdministratorAdded) {
                        continue;
                    }
                    platformAdministratorAdded = true;
                }
            }
            filtered.add(group);
        }
        return filtered;
    }

    private static boolean isAdministratorGroupName(String name) {
        return StringUtils.hasText(name)
                && OrganizationWorkspaceProvisioner.ADMINISTRATOR_GROUP_NAME.equalsIgnoreCase(name.trim());
    }

    private AdministratorRoleScopePolicy.PortalScope resolvePortalScopeForSession(String username) {
        return AdministratorRoleScopePolicy.portalScopeFor(
                organizationWorkspaceAccessSupport.sessionOrganizationId(username).orElse(null));
    }

    private List<UserRoleDto> filterRoleDtosForPortalScope(
            List<UserRoleDto> roleDtos,
            AdministratorRoleScopePolicy.PortalScope scope) {
        if (roleDtos == null || roleDtos.isEmpty()) {
            return List.of();
        }
        List<UserRoleDto> filtered = new ArrayList<>();
        for (UserRoleDto roleDto : roleDtos) {
            if (roleDto == null || !StringUtils.hasText(roleDto.getRole())) {
                continue;
            }
            if (AdministratorRoleScopePolicy.isEffectiveForScope(roleDto.getRole(), scope)) {
                filtered.add(roleDto);
            }
        }
        return filtered;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<UserGroupDto> userGroups) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (UserGroupDto userGroup : userGroups) {
            String userRoles = userGroup.getUserRoleDtoSet() != null ? 
                userGroup.getUserRoleDtoSet().stream()
                    .map(role -> role.getRole())
                    .collect(Collectors.joining("; ")) : "";

            sb.append(userGroup.getId()).append(",")
                    .append(safe(userGroup.getName())).append(",")
                    .append(safe(userGroup.getDescription())).append(",")
                    .append(safe(userRoles)).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<UserGroupDto> userGroups) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("User Groups");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (UserGroupDto userGroup : userGroups) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(userGroup.getId());
            row.createCell(1).setCellValue(safe(userGroup.getName()));
            row.createCell(2).setCellValue(safe(userGroup.getDescription()));

            String userRoles = userGroup.getUserRoleDtoSet() != null ? 
                userGroup.getUserRoleDtoSet().stream()
                    .map(role -> role.getRole())
                    .collect(Collectors.joining("; ")) : "";
            row.createCell(3).setCellValue(safe(userRoles));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<UserGroupDto> userGroups) throws DocumentException {
        List<String[]> rows = new ArrayList<>();
        for (UserGroupDto userGroup : userGroups) {
            String userRoles = userGroup.getUserRoleDtoSet() != null ?
                    userGroup.getUserRoleDtoSet().stream()
                            .map(role -> role.getRole())
                            .collect(Collectors.joining("; ")) : "";
            rows.add(new String[]{
                    String.valueOf(userGroup.getId()),
                    safe(userGroup.getName()),
                    safe(userGroup.getDescription()),
                    safe(userRoles)
            });
        }
        return LdmsPdfReportWriter.write(LdmsExportReport.builder()
                .title("User Groups")
                .reportCode("USR-GRP")
                .subtitle("User group registry export")
                .columnHeaders(HEADERS)
                .rows(rows)
                .landscape(true)
                .build());
    }

    @Override
    public ImportSummary importUserGroupsFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            List<CSVRecord> records = csvParser.getRecords(); // Get records first
            total = records.size();

            for (CSVRecord record : records) {
                try {
                    CreateUserGroupRequest request = new CreateUserGroupRequest();
                    request.setName(record.get("NAME"));
                    request.setDescription(record.get("DESCRIPTION"));

                    UserGroupResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        errors.add("Row " + record.getRecordNumber() + ": " + response.getMessage());
                    }

                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + record.getRecordNumber() + ": Unexpected error - " + e.getMessage());
                }
            }
        }

        // Determine status code and success flag based on import results
        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0
                ? "Import completed successfully. " + success + " out of " + total + " user groups imported."
                : "Import failed. No user groups were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private UserGroupResponse buildUserGroupResponse(int statusCode, boolean isSuccess, String message,
                                                     UserGroupDto userGroupDto, List<UserGroupDto> userGroupDtoList,
                                                     Page<UserGroupDto> userGroupDtoPage) {

        UserGroupResponse userGroupResponse = new UserGroupResponse();

        userGroupResponse.setStatusCode(statusCode);
        userGroupResponse.setSuccess(isSuccess);
        userGroupResponse.setMessage(message);
        userGroupResponse.setUserGroupDto(userGroupDto);
        userGroupResponse.setUserGroupDtoList(userGroupDtoList);
        userGroupResponse.setUserGroupDtoPage(userGroupDtoPage);

        return userGroupResponse;
    }

    private UserGroupResponse buildUserGroupResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                     UserGroupDto userGroupDto, List<UserGroupDto> userGroupDtoList,
                                                     List<String> errorMessages) {

        UserGroupResponse userGroupResponse = new UserGroupResponse();

        userGroupResponse.setStatusCode(statusCode);
        userGroupResponse.setSuccess(isSuccess);
        userGroupResponse.setMessage(message);
        userGroupResponse.setUserGroupDto(userGroupDto);
        userGroupResponse.setUserGroupDtoList(userGroupDtoList);
        userGroupResponse.setErrorMessages(errorMessages);

        return userGroupResponse;
    }
}
