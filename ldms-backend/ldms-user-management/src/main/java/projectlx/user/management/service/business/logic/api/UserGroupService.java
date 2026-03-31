package projectlx.user.management.service.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.dtos.UserGroupDto;
import projectlx.user.management.service.utils.requests.AddUserToUserGroupRequest;
import projectlx.user.management.service.utils.requests.AssignUserRoleToUserGroupRequest;
import projectlx.user.management.service.utils.requests.CreateUserGroupRequest;
import projectlx.user.management.service.utils.requests.EditUserGroupRequest;
import projectlx.user.management.service.utils.requests.RemoveUserRolesFromUserGroupRequest;
import projectlx.user.management.service.utils.requests.UserGroupMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserGroupResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface UserGroupService {
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
    byte[] exportToCsv(List<UserGroupDto> userGroups);
    byte[] exportToExcel(List<UserGroupDto> userGroups) throws IOException;
    byte[] exportToPdf(List<UserGroupDto> userGroups) throws DocumentException;
    ImportSummary importUserGroupsFromCsv(InputStream csvInputStream) throws IOException;
}
