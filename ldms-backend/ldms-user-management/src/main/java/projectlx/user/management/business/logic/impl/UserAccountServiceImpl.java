package projectlx.user.management.business.logic.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.auditable.api.UserAccountServiceAuditable;
import projectlx.user.management.business.logic.api.UserAccountService;
import projectlx.user.management.business.validator.api.UserAccountServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.User;
import projectlx.user.management.model.UserAccount;
import projectlx.user.management.repository.UserAccountRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.repository.specification.UserAccountSpecification;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.dtos.UserAccountDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.generators.AccountNumberAndReferencesGenerator;
import projectlx.user.management.utils.requests.CreateUserAccountRequest;
import projectlx.user.management.utils.requests.EditUserAccountRequest;
import projectlx.user.management.utils.requests.UserAccountMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserAccountResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

@Data
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final ModelMapper modelMapper;
    private final MessageService messageService;
    private final UserAccountRepository userAccountRepository;
    private final UserRepository userRepository;
    private final UserAccountServiceAuditable userAccountServiceAuditable;
    private final UserAccountServiceValidator userAccountServiceValidator;

    private static final String[] HEADERS = {
            "ID", "ACCOUNT NUMBER", "PHONE NUMBER", "IS ACCOUNT LOCKED", "LAST LOGIN AT", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
    };

    @Override
    public UserAccountResponse create(CreateUserAccountRequest createUserAccountRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userAccountServiceValidator.isCreateUserRequestValid(createUserAccountRequest, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_ACCOUNT_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildUserAccountResponse(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        String normalizedPhoneNumber = createUserAccountRequest.getPhoneNumber() != null
                ? createUserAccountRequest.getPhoneNumber().trim()
                : null;
        createUserAccountRequest.setPhoneNumber(normalizedPhoneNumber);

        Optional<UserAccount> userAccountRetrieved = userAccountRepository.findByPhoneNumberAndEntityStatusNot(
                normalizedPhoneNumber, EntityStatus.DELETED);

        if (userAccountRetrieved.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);

            return buildUserAccountResponse(400, false, message, null, null,
                    null);
        }

        Optional<User> userRetrieved = userRepository.findByIdAndEntityStatusNot(createUserAccountRequest.getUserId(),
                EntityStatus.DELETED);

        if (userRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserAccountResponse(404, false, message, null, null,
                    null);
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Optional<UserAccount> deletedUserAccount = userAccountRepository.findByPhoneNumber(normalizedPhoneNumber)
                .filter(account -> account.getEntityStatus() == EntityStatus.DELETED);

        UserAccount userAccountSaved;
        if (deletedUserAccount.isPresent()) {
            UserAccount userAccountToBeReactivated = deletedUserAccount.get();
            userAccountToBeReactivated.setUser(userRetrieved.get());
            userAccountToBeReactivated.setPhoneNumber(normalizedPhoneNumber);
            userAccountToBeReactivated.setEntityStatus(EntityStatus.ACTIVE);
            userAccountSaved = userAccountServiceAuditable.update(userAccountToBeReactivated, locale, username);
        } else {
            createUserAccountRequest.setAccountNumber(AccountNumberAndReferencesGenerator.getAccountNumber());
            UserAccount userAccountToBeSaved = modelMapper.map(createUserAccountRequest, UserAccount.class);
            userAccountToBeSaved.setUser(userRetrieved.get());
            userAccountSaved = userAccountServiceAuditable.create(userAccountToBeSaved, locale, username);
        }

        UserAccountDto userAccountDtoReturned = modelMapper.map(userAccountSaved, UserAccountDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserAccountResponse(201, true, message, userAccountDtoReturned, null,
                null);
    }

    @Override
    public UserAccountResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userAccountServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserAccountResponse(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<UserAccount> userAccountRetrieved = userAccountRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userAccountRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserAccountResponse(404, false, message, null, null,
                    null);
        }

        UserAccount userAccountReturned = userAccountRetrieved.get();

        UserAccountDto userAccountDto = modelMapper.map(userAccountReturned, UserAccountDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserAccountResponse(200, true, message, userAccountDto, null,
                null);
    }

    @Override
    public UserAccountResponse findAllAsList(String username, Locale locale) {

        String message = "";

        List<UserAccount> userAccountList = userAccountRepository.findByEntityStatusNot(EntityStatus.DELETED);

        if(userAccountList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildUserAccountResponse(404, false, message, null,
                    null, null);
        }

        List<UserAccountDto> userAccountDtoList = modelMapper.map(userAccountList, new TypeToken<List<UserAccount>>(){}.
                getType());

        message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserAccountResponse(200, true, message, null, userAccountDtoList,
                null);
    }

    @Override
    public UserAccountResponse update(EditUserAccountRequest editUserAccountRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = userAccountServiceValidator.isRequestValidForEditing(editUserAccountRequest, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_ACCOUNT_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildUserAccountResponse(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<UserAccount> userAddressRetrieved = userAccountRepository.findByIdAndEntityStatusNot(editUserAccountRequest.getId(),
                EntityStatus.DELETED);

        if (userAddressRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserAccountResponse(400, false, message, null, null,
                    null);
        }

        UserAccount userAccountToBeEdited = userAddressRetrieved.get();

        UserAccount userAccountEdited = userAccountServiceAuditable.update(userAccountToBeEdited, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserAccountDto userAccountDtoReturned = modelMapper.map(userAccountEdited, UserAccountDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserAccountResponse(200, true, message, userAccountDtoReturned, null,
                null);
    }

    @Override
    public UserAccountResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userAccountServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildUserAccountResponse(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<UserAccount> userAccountRetrieved = userAccountRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userAccountRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserAccountResponse(404, false, message, null, null,
                    null);
        }

        UserAccount userAccountToBeDeleted = userAccountRetrieved.get();
        userAccountToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        UserAccount userAccountDeleted = userAccountServiceAuditable.delete(userAccountToBeDeleted, locale, username);

        UserAccountDto userAccountDtoReturned = modelMapper.map(userAccountDeleted, UserAccountDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserAccountResponse(200, true, message, userAccountDtoReturned, null,
                null);
    }

    @Override
    public UserAccountResponse findByMultipleFilters(UserAccountMultipleFiltersRequest userAccountMultipleFiltersRequest, String username, Locale locale) {

        String message = "";

        Specification<UserAccount> spec = null;
        spec = addToSpec(spec, UserAccountSpecification::deleted);

        ValidatorDto validatorDto = userAccountServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(
                userAccountMultipleFiltersRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildUserAccountResponse(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(userAccountMultipleFiltersRequest.getPage(),
                userAccountMultipleFiltersRequest.getSize());

        ValidatorDto phoneNumberValidatorDto = userAccountServiceValidator.isStringValid(
                userAccountMultipleFiltersRequest.getPhoneNumber(), locale);

        if (phoneNumberValidatorDto.getSuccess()) {

            spec = addToSpec(userAccountMultipleFiltersRequest.getPhoneNumber(), spec,
                    UserAccountSpecification::phoneNumberLike);
        }

        ValidatorDto accountNumberValidatorDto = userAccountServiceValidator.isStringValid(
                userAccountMultipleFiltersRequest.getAccountNumber(), locale);

        if (accountNumberValidatorDto.getSuccess()) {

            spec = addToSpec(userAccountMultipleFiltersRequest.getAccountNumber(), spec,
                    UserAccountSpecification::accountNumberLike);
        }

        ValidatorDto accountLockedValidatorDto = userAccountServiceValidator.isBooleanValid(
                userAccountMultipleFiltersRequest.getIsAccountLocked(), locale);

        if (accountLockedValidatorDto.getSuccess()) {

            spec = addToSpec(userAccountMultipleFiltersRequest.getIsAccountLocked(), spec, UserAccountSpecification::isAccountLocked);
        }

        ValidatorDto searchValueValidatorDto = userAccountServiceValidator.isStringValid(
                userAccountMultipleFiltersRequest.getSearchValue(), locale);

        if (searchValueValidatorDto.getSuccess()) {

            spec = addToSpec(userAccountMultipleFiltersRequest.getSearchValue(), spec, UserAccountSpecification::any);
        }

        Page<UserAccount> result = userAccountRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserAccountResponse(404, false, message,null, null,
                    null);
        }

        Page<UserAccountDto> userAccountDtoPage = convertUserAccountEntityToUserAccountDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ACCOUNT_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserAccountResponse(200, true, message,null, null,
                userAccountDtoPage);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<UserAccountDto> userAccounts) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (UserAccountDto userAccount : userAccounts) {
            sb.append(userAccount.getId()).append(",")
                    .append(safe(userAccount.getAccountNumber())).append(",")
                    .append(safe(userAccount.getPhoneNumber())).append(",")
                    .append(userAccount.getIsAccountLocked()).append(",")
                    .append(userAccount.getLastLoginAt()).append(",")
                    .append(userAccount.getCreatedAt()).append(",")
                    .append(userAccount.getUpdatedAt()).append(",")
                    .append(userAccount.getEntityStatus()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<UserAccountDto> userAccounts) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("User Accounts");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (UserAccountDto userAccount : userAccounts) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(userAccount.getId());
            row.createCell(1).setCellValue(safe(userAccount.getAccountNumber()));
            row.createCell(2).setCellValue(safe(userAccount.getPhoneNumber()));
            row.createCell(3).setCellValue(userAccount.getIsAccountLocked() != null ? userAccount.getIsAccountLocked() : false);
            row.createCell(4).setCellValue(userAccount.getLastLoginAt() != null ? userAccount.getLastLoginAt().toString() : "");
            row.createCell(5).setCellValue(userAccount.getCreatedAt() != null ? userAccount.getCreatedAt().toString() : "");
            row.createCell(6).setCellValue(userAccount.getUpdatedAt() != null ? userAccount.getUpdatedAt().toString() : "");
            row.createCell(7).setCellValue(userAccount.getEntityStatus() != null ? userAccount.getEntityStatus().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<UserAccountDto> userAccounts) throws DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("USER ACCOUNT EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (UserAccountDto userAccount : userAccounts) {
            table.addCell(String.valueOf(userAccount.getId()));
            table.addCell(safe(userAccount.getAccountNumber()));
            table.addCell(safe(userAccount.getPhoneNumber()));
            table.addCell(String.valueOf(userAccount.getIsAccountLocked() != null ? userAccount.getIsAccountLocked() : false));
            table.addCell(userAccount.getLastLoginAt() != null ? userAccount.getLastLoginAt().toString() : "");
            table.addCell(userAccount.getCreatedAt() != null ? userAccount.getCreatedAt().toString() : "");
            table.addCell(userAccount.getUpdatedAt() != null ? userAccount.getUpdatedAt().toString() : "");
            table.addCell(userAccount.getEntityStatus() != null ? userAccount.getEntityStatus().toString() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public ImportSummary importUserAccountsFromCsv(InputStream csvInputStream) throws IOException {
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
                    CreateUserAccountRequest request = new CreateUserAccountRequest();
                    request.setPhoneNumber(record.get("PHONE NUMBER"));
                    request.setUserId(Long.parseLong(record.get("USER ID")));
                    request.setAccountNumber(AccountNumberAndReferencesGenerator.getAccountNumber());
                    request.setIsAccountLocked(Boolean.parseBoolean(record.get("IS ACCOUNT LOCKED")));

                    UserAccountResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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

        return new ImportSummary(total, success, failed, errors);
    }

    @Override
    public ImportSummary importUserAccountsFromExcel(InputStream excelInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0;
        int rowNumber = 1; // Assuming headers are at row 0

        Workbook workbook = new XSSFWorkbook(excelInputStream);
        Sheet sheet = workbook.getSheetAt(0);

        for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
            Row row = sheet.getRow(rowIdx);

            if (row == null) continue;

            try {
                CreateUserAccountRequest request = new CreateUserAccountRequest();
                request.setPhoneNumber(getCellValue(row.getCell(0)));
                request.setUserId(Long.parseLong(getCellValue(row.getCell(1))));
                request.setAccountNumber(AccountNumberAndReferencesGenerator.getAccountNumber());
                request.setIsAccountLocked(Boolean.parseBoolean(getCellValue(row.getCell(2))));

                UserAccountResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                if (response.isSuccess()) {
                    success++;
                } else {
                    failed++;
                    errors.add("Row " + rowNumber + ": " + response.getMessage());
                }

            } catch (Exception e) {
                failed++;
                errors.add("Row " + rowNumber + ": Unexpected error - " + e.getMessage());
            }

            rowNumber++;
        }

        workbook.close();
        return new ImportSummary(rowNumber - 1, success, failed, errors);
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Convert numeric to string without decimal if it's a whole number
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        return String.valueOf((long) value);
                    }
                    return String.valueOf(value);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private Specification<UserAccount> addToSpec(Specification<UserAccount> spec,
                                                 Function<EntityStatus, Specification<UserAccount>> predicateMethod) {
        Specification<UserAccount> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<UserAccount> addToSpec(final String aString, Specification<UserAccount> spec, Function<String,
            Specification<UserAccount>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<UserAccount> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<UserAccount> addToSpec(final Boolean aBoolean, Specification<UserAccount> spec, Function<Boolean,
            Specification<UserAccount>> predicateMethod) {
        if (aBoolean != null) {
            Specification<UserAccount> localSpec = Specification.where(predicateMethod.apply(aBoolean));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Page<UserAccountDto> convertUserAccountEntityToUserAccountDto(Page<UserAccount> userAccountPage) {

        List<UserAccount> userAccountList = userAccountPage.getContent();
        List<UserAccountDto> userAccountDtoList = new ArrayList<>();

        for (UserAccount userAccount : userAccountPage) {
            UserAccountDto userAccountDto = modelMapper.map(userAccount, UserAccountDto.class);
            userAccountDtoList.add(userAccountDto);
        }

        int page = userAccountPage.getNumber();
        int size = userAccountPage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableUserAccounts = PageRequest.of(page, size);

        return new PageImpl<UserAccountDto>(userAccountDtoList, pageableUserAccounts, userAccountPage.getTotalElements());
    }

    private UserAccountResponse buildUserAccountResponse(int statusCode, boolean isSuccess, String message,
                                                         UserAccountDto userAccountDto, List<UserAccountDto> userAccountDtoList,
                                                         Page<UserAccountDto> userAccountDtoPage, List<String> errorMessages) {

        UserAccountResponse userAccountResponse = new UserAccountResponse();
        userAccountResponse.setStatusCode(statusCode);
        userAccountResponse.setSuccess(isSuccess);
        userAccountResponse.setMessage(message);
        userAccountResponse.setUserAccountDto(userAccountDto);
        userAccountResponse.setUserAccountDtoList(userAccountDtoList);
        userAccountResponse.setUserAccountDtoPage(userAccountDtoPage);
        userAccountResponse.setErrorMessages(errorMessages);

        return userAccountResponse;
    }

    private UserAccountResponse buildUserAccountResponse(int statusCode, boolean isSuccess, String message,
                                                         UserAccountDto userAccountDto, List<UserAccountDto> userAccountDtoList,
                                                         Page<UserAccountDto> userAccountDtoPage) {
        return buildUserAccountResponse(statusCode, isSuccess, message, userAccountDto, userAccountDtoList, userAccountDtoPage, null);
    }
}
