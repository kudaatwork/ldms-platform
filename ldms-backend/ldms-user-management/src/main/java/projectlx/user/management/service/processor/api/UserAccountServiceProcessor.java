package projectlx.user.management.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.utils.requests.EditUserAccountRequest;
import projectlx.user.management.utils.requests.UserAccountMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserAccountResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public interface UserAccountServiceProcessor {
    UserAccountResponse create(CreateUserAccountRequest createUserAccountRequest, Locale locale, String username);
    UserAccountResponse findById(Long id, Locale locale, String username);
    UserAccountResponse findAllAsList(String username, Locale locale);
    UserAccountResponse update(EditUserAccountRequest editUserAccountRequest, String username, Locale locale);
    UserAccountResponse delete(Long id, Locale locale, String username);
    UserAccountResponse findByMultipleFilters(UserAccountMultipleFiltersRequest userAccountMultipleFiltersRequest,
                                              String username, Locale locale);
    ImportSummary importUserAccountsFromCsv(InputStream csvInputStream) throws IOException;
    ImportSummary importUserAccountsFromExcel(InputStream excelInputStream) throws IOException;
    byte[] exportToCsv(UserAccountMultipleFiltersRequest filters, String username, Locale locale);
    byte[] exportToExcel(UserAccountMultipleFiltersRequest filters, String username, Locale locale) throws IOException;
    byte[] exportToPdf(UserAccountMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException;
}
