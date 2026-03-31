package projectlx.user.management.service.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.dtos.UserTypeDto;
import projectlx.user.management.service.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.service.utils.requests.EditUserTypeRequest;
import projectlx.user.management.service.utils.requests.UserTypeMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserTypeResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface UserTypeService {
    UserTypeResponse create(CreateUserTypeRequest createUserTypeRequest, Locale locale, String username);
    UserTypeResponse findById(Long id, Locale locale, String username);
    UserTypeResponse findAllAsList(String username, Locale locale);
    UserTypeResponse update(EditUserTypeRequest editUserTypeRequest, String username, Locale locale);
    UserTypeResponse delete(Long id, Locale locale, String username);
    UserTypeResponse findByMultipleFilters(UserTypeMultipleFiltersRequest userTypeMultipleFiltersRequest,
                                           String username, Locale locale);
    byte[] exportToCsv(List<UserTypeDto> userTypes);
    byte[] exportToExcel(List<UserTypeDto> userTypes) throws IOException;
    byte[] exportToPdf(List<UserTypeDto> userTypes) throws DocumentException;
    ImportSummary importUserTypesFromCsv(InputStream csvInputStream) throws IOException;
}
