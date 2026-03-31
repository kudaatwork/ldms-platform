package projectlx.user.management.service.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.service.utils.requests.EditUserTypeRequest;
import projectlx.user.management.service.utils.requests.UserTypeMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserTypeResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public interface UserTypeServiceProcessor {
    UserTypeResponse create(CreateUserTypeRequest createUserTypeRequest, Locale locale, String username);
    UserTypeResponse findById(Long id, Locale locale, String username);
    UserTypeResponse findAllAsList(String username, Locale locale);
    UserTypeResponse update(EditUserTypeRequest editUserTypeRequest, String username, Locale locale);
    UserTypeResponse delete(Long id, Locale locale, String username);
    UserTypeResponse findByMultipleFilters(UserTypeMultipleFiltersRequest userTypeMultipleFiltersRequest,
                                           String username, Locale locale);
    byte[] exportToCsv(UserTypeMultipleFiltersRequest filters, String username, Locale locale);
    byte[] exportToExcel(UserTypeMultipleFiltersRequest filters, String username, Locale locale) throws IOException;
    byte[] exportToPdf(UserTypeMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException;
    ImportSummary importUserTypesFromCsv(InputStream csvInputStream) throws IOException;
}
