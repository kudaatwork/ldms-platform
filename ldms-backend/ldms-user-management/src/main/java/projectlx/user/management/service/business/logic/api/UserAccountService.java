package projectlx.user.management.service.business.logic.api;

import com.lowagie.text.DocumentException;
import projectlx.user.management.service.utils.dtos.ImportSummary;
import projectlx.user.management.service.utils.dtos.UserAccountDto;
import projectlx.user.management.service.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.service.utils.requests.EditUserAccountRequest;
import projectlx.user.management.service.utils.requests.UserAccountMultipleFiltersRequest;
import projectlx.user.management.service.utils.responses.UserAccountResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public interface UserAccountService {
    UserAccountResponse create(CreateUserAccountRequest createUserAccountRequest, Locale locale, String username);
    UserAccountResponse findById(Long id, Locale locale, String username);
    UserAccountResponse findAllAsList(String username, Locale locale);
    UserAccountResponse update(EditUserAccountRequest editUserAccountRequest, String username, Locale locale);
    UserAccountResponse delete(Long id, Locale locale, String username);
    UserAccountResponse findByMultipleFilters(UserAccountMultipleFiltersRequest userAccountMultipleFiltersRequest,
                                              String username, Locale locale);
    byte[] exportToCsv(List<UserAccountDto> userAccounts);
    byte[] exportToExcel(List<UserAccountDto> userAccounts) throws IOException;
    byte[] exportToPdf(List<UserAccountDto> userAccounts) throws DocumentException;
    ImportSummary importUserAccountsFromCsv(InputStream csvInputStream) throws IOException;
    ImportSummary importUserAccountsFromExcel(InputStream excelInputStream) throws IOException;
}
