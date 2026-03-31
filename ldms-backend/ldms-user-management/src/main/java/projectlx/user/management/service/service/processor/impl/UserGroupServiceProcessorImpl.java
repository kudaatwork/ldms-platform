package projectlx.user.management.service.service.processor.impl;

import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Page;
import projectlx.user.management.service.business.logic.api.UserGroupService;
import projectlx.user.management.service.service.processor.api.UserGroupServiceProcessor;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.dtos.UserGroupDto;
import projectlx.user.management.service.utils.requests.AddUserToUserGroupRequest;
import projectlx.user.management.service.utils.requests.AssignUserRoleToUserGroupRequest;
import projectlx.user.management.service.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.service.utils.requests.EditUserGroupRequest;
import projectlx.user.management.service.utils.requests.RemoveUserRolesFromUserGroupRequest;
import projectlx.user.management.service.utils.requests.UserGroupMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserGroupResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class UserGroupServiceProcessorImpl implements UserGroupServiceProcessor {

    private final UserGroupService userGroupService;
    private final Logger logger = LoggerFactory.getLogger(UserAddressServiceProcessorImpl.class);

    @Override
    public UserGroupResponse create(CreateUserGroupRequest createUserGroupRequest, Locale locale, String username) {

        logger.info("Incoming request to create a user group : {}", createUserGroupRequest);

        UserGroupResponse userGroupResponse = userGroupService.create(createUserGroupRequest,
                locale, username);

        logger.info("Outgoing response after creating a user group : {}. Status Code: {}. Message: {}",
                userGroupResponse, userGroupResponse.getStatusCode(), userGroupResponse.getMessage());

        return userGroupResponse;
    }

    @Override
    public UserGroupResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find a user group by id: {}", id);

        UserGroupResponse userGroupResponse = userGroupService.findById(id, locale, username);

        logger.info("Outgoing response after finding a user group by id : {}. Status Code: {}. Message: {}",
                userGroupResponse, userGroupResponse.getStatusCode(), userGroupResponse.getMessage());

        return userGroupResponse;
    }

    @Override
    public UserGroupResponse findAllAsList(String username, Locale locale) {

        logger.info("Incoming request to find all user groups as a list");

        UserGroupResponse userGroupResponse = userGroupService.findAllAsList(username, locale);

        logger.info("Outgoing response after finding all user groups as a list : {}. Status Code: {}. Message: {}",
                userGroupResponse, userGroupResponse.getStatusCode(), userGroupResponse.getMessage());

        return userGroupResponse;
    }

    @Override
    public UserGroupResponse update(EditUserGroupRequest editUserGroupRequest, String username, Locale locale) {

        logger.info("Incoming request to update a user group : {}", editUserGroupRequest);

        UserGroupResponse userGroupResponse = userGroupService.update(editUserGroupRequest, username, locale);

        logger.info("Outgoing response after updating a user group : {}. Status Code: {}. Message: {}",
                userGroupResponse, userGroupResponse.getStatusCode(), userGroupResponse.getMessage());

        return userGroupResponse;
    }

    @Override
    public UserGroupResponse delete(Long id, Locale locale, String username) {

        logger.info("Incoming request to delete a user group with the id : {}", id);

        UserGroupResponse userGroupResponse = userGroupService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a user group: {}. Status Code: {}. Message: {}", userGroupResponse,
                userGroupResponse.getStatusCode(), userGroupResponse.getMessage());

        return userGroupResponse;
    }

    @Override
    public UserGroupResponse findByMultipleFilters(UserGroupMultipleFiltersRequest userGroupMultipleFiltersRequest,
                                                   String username, Locale locale) {

        logger.info("Incoming request to find a user group using multiple filters : {}", userGroupMultipleFiltersRequest);

        UserGroupResponse userGroupResponse = userGroupService.findByMultipleFilters(userGroupMultipleFiltersRequest,
                username, locale);

        logger.info("Outgoing response after finding a user group using multiple filters: {}. Status Code: {}. Message: {}",
                userGroupResponse, userGroupResponse.getStatusCode(), userGroupResponse.getMessage());

        return userGroupResponse;
    }

    @Override
    public UserGroupResponse assignUserRoleToUserGroup(AssignUserRoleToUserGroupRequest assignUserRoleToUserGroupRequest, Locale locale, String username) {

        logger.info("Incoming request to assign user role(s) to a user group : {}", assignUserRoleToUserGroupRequest);

        UserGroupResponse userGroupResponse = userGroupService.assignUserRoleToUserGroup(assignUserRoleToUserGroupRequest,
                locale, username);

        logger.info("Outgoing response after assigning user role(s) to a user group: {}. Status Code: {}. Message: {}",
                userGroupResponse, userGroupResponse.getStatusCode(), userGroupResponse.getMessage());

        return userGroupResponse;
    }

    @Override
    public UserGroupResponse removeUserRolesFromUserGroup(RemoveUserRolesFromUserGroupRequest removeUserRolesFromUserGroupRequest, Locale locale, String username) {

        logger.info("Incoming request to remove user role(s) to a user group : {}", removeUserRolesFromUserGroupRequest);

        UserGroupResponse userGroupResponse = userGroupService.removeUserRolesFromUserGroup(removeUserRolesFromUserGroupRequest,
                locale, username);

        logger.info("Outgoing response after removing user role(s) to a user group: {}. Status Code: {}. Message: {}",
                userGroupResponse, userGroupResponse.getStatusCode(), userGroupResponse.getMessage());

        return userGroupResponse;
    }

    @Override
    public UserGroupResponse addUserGroupToUser(AddUserToUserGroupRequest addUserToUserGroupRequest, Locale locale, String username) {

        logger.info("Incoming request to add user group to a user : {}", addUserToUserGroupRequest);

        UserGroupResponse userGroupResponse = userGroupService.addUserGroupToUser(addUserToUserGroupRequest,
                locale, username);

        logger.info("Outgoing response after adding a user group to a user: {}. Status Code: {}. Message: {}",
                userGroupResponse, userGroupResponse.getStatusCode(), userGroupResponse.getMessage());

        return userGroupResponse;
    }

    @Override
    public byte[] exportToCsv(UserGroupMultipleFiltersRequest filters, String username, Locale locale) {
        logger.info("Incoming request to export user groups to CSV using filters: {}", filters);

        UserGroupResponse userGroupResponse = userGroupService.findByMultipleFilters(filters, username, locale);

        List<UserGroupDto> userGroupList = Optional.ofNullable(userGroupResponse.getUserGroupDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] csvData = userGroupService.exportToCsv(userGroupList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(UserGroupMultipleFiltersRequest filters, String username, Locale locale) throws IOException {
        logger.info("Incoming request to export user groups to Excel using filters: {}", filters);

        UserGroupResponse userGroupResponse = userGroupService.findByMultipleFilters(filters, username, locale);

        List<UserGroupDto> userGroupList = Optional.ofNullable(userGroupResponse.getUserGroupDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] excelData = userGroupService.exportToExcel(userGroupList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(UserGroupMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException {
        logger.info("Incoming request to export user groups to PDF using filters: {}", filters);

        UserGroupResponse userGroupResponse = userGroupService.findByMultipleFilters(filters, username, locale);

        List<UserGroupDto> userGroupList = Optional.ofNullable(userGroupResponse.getUserGroupDtoPage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] pdfData = userGroupService.exportToPdf(userGroupList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importUserGroupsFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import user groups from CSV");

        ImportSummary importSummary = userGroupService.importUserGroupsFromCsv(csvInputStream);

        logger.info("Outgoing response after importing user groups from CSV: Total: {}, Success: {}, Failed: {}",
                importSummary.total, importSummary.success, importSummary.failed);

        return importSummary;
    }
}
