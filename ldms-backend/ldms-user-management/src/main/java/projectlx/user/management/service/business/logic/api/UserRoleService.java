package projectlx.user.management.service.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.dtos.UserRoleDto;
import projectlx.user.management.service.utils.requests.CreateUserRoleRequest;
import projectlx.user.management.service.utils.requests.EditUserRoleRequest;
import projectlx.user.management.service.utils.requests.UserRoleMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserRoleResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface UserRoleService {
    UserRoleResponse create(CreateUserRoleRequest createUserRoleRequest, Locale locale, String username);
    UserRoleResponse findById(Long id, Locale locale, String username);
    UserRoleResponse findAllAsList(String username, Locale locale);
    UserRoleResponse update(EditUserRoleRequest editUserRoleRequest, String username, Locale locale);
    UserRoleResponse delete(Long id, Locale locale, String username);
    UserRoleResponse findByMultipleFilters(UserRoleMultipleFiltersRequest userRoleMultipleFiltersRequest,
                                           String username, Locale locale);
    byte[] exportToCsv(List<UserRoleDto> userRoles);
    byte[] exportToExcel(List<UserRoleDto> userRoles) throws IOException;
    byte[] exportToPdf(List<UserRoleDto> userRoles) throws DocumentException;
    ImportSummary importUserRolesFromCsv(InputStream csvInputStream) throws IOException;
}
