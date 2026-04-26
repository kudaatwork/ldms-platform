package projectlx.user.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.requests.AddUserToUserGroupRequest;
import projectlx.user.management.utils.requests.AssignUserRoleToUserGroupRequest;
import projectlx.user.management.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.utils.requests.EditUserGroupRequest;
import projectlx.user.management.utils.requests.RemoveUserRolesFromUserGroupRequest;
import projectlx.user.management.utils.requests.UserGroupMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserGroupResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public interface UserGroupServiceProcessor {
    UserGroupResponse create(CreateUserGroupRequest createUserGroupRequest, Locale locale, String username);
    UserGroupResponse findById(Long id, Locale locale, String username);
    UserGroupResponse findAllAsList(String username, Locale locale);
    UserGroupResponse update(EditUserGroupRequest editUserGroupRequest, String username, Locale locale);
    UserGroupResponse delete(Long id, Locale locale, String username);
    UserGroupResponse findByMultipleFilters(UserGroupMultipleFiltersRequest userGroupMultipleFiltersRequest,
                                            String username, Locale locale);
    UserGroupResponse assignUserRoleToUserGroup(AssignUserRoleToUserGroupRequest assignUserRoleToUserGroupRequest,
                                                Locale locale, String username);
    UserGroupResponse removeUserRolesFromUserGroup(RemoveUserRolesFromUserGroupRequest removeUserRolesFromUserGroupRequest,
                                                   Locale locale, String username);
    UserGroupResponse addUserGroupToUser(AddUserToUserGroupRequest addUserToUserGroupRequest, Locale locale, String username);
    byte[] exportToCsv(UserGroupMultipleFiltersRequest filters, String username, Locale locale);
    byte[] exportToExcel(UserGroupMultipleFiltersRequest filters, String username, Locale locale) throws IOException;
    byte[] exportToPdf(UserGroupMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException;
    ImportSummary importUserGroupsFromCsv(InputStream csvInputStream) throws IOException;
}
