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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.auditable.api.UserRoleServiceAuditable;
import projectlx.user.management.business.logic.api.UserRoleService;
import projectlx.user.management.business.validator.api.UserRoleServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserRole;
import projectlx.user.management.repository.UserRoleRepository;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.repository.specification.UserRoleSpecification;
import projectlx.user.management.utils.dtos.UserRoleDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserRoleRequest;
import projectlx.user.management.utils.requests.EditUserRoleRequest;
import projectlx.user.management.utils.requests.UserRoleMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserRoleResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import projectlx.user.management.utils.dtos.ImportSummary;

@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleServiceValidator userRoleServiceValidator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final UserRoleServiceAuditable userRoleServiceAuditable;

    private static final String[] HEADERS = {
            "ID", "ROLE", "DESCRIPTION", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
    };

    @Override
    public UserRoleResponse create(CreateUserRoleRequest createUserRoleRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userRoleServiceValidator.isCreateUserRoleRequestValid(createUserRoleRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_ROLE_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildUserRoleResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        String normalizedRole = createUserRoleRequest.getRole().toUpperCase();
        Optional<UserRole> userRoleRetrieved =
                userRoleRepository.findByRoleAndEntityStatusNot(normalizedRole, EntityStatus.DELETED);

        if (userRoleRetrieved.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);

            return buildUserRoleResponse(400, false, message, null,
                    null, null);
        }

        Optional<UserRole> deletedUserRole = userRoleRepository.findByRole(normalizedRole)
                .filter(role -> role.getEntityStatus() == EntityStatus.DELETED);

        createUserRoleRequest.setRole(normalizedRole);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserRole userRoleSaved;
        if (deletedUserRole.isPresent()) {
            UserRole userRoleToBeReactivated = deletedUserRole.get();
            userRoleToBeReactivated.setRole(normalizedRole);
            userRoleToBeReactivated.setDescription(createUserRoleRequest.getDescription());
            userRoleToBeReactivated.setEntityStatus(EntityStatus.ACTIVE);
            userRoleSaved = userRoleServiceAuditable.update(userRoleToBeReactivated, locale, username);
        } else {
            UserRole userRoleToBeSaved = modelMapper.map(createUserRoleRequest, UserRole.class);
            userRoleSaved = userRoleServiceAuditable.create(userRoleToBeSaved, locale, username);
        }

        UserRoleDto useRoleDtoReturned = modelMapper.map(userRoleSaved, UserRoleDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserRoleResponse(201, true, message, useRoleDtoReturned, null,
                null);
    }

    @Override
    public UserRoleResponse findById(Long id, Locale locale, String username) {
        String message = "";

        ValidatorDto validatorDto = userRoleServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserRoleResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<UserRole> userRoleRetrieved = userRoleRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userRoleRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserRoleResponse(404, false, message, null, null,
                    null);
        }

        UserRole userRoleReturned = userRoleRetrieved.get();
        UserRoleDto userRoleDto = modelMapper.map(userRoleReturned, UserRoleDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserRoleResponse(200, true, message, userRoleDto, null,
                null);
    }

    @Override
    public UserRoleResponse findAllAsList(String username, Locale locale) {
        String message = "";

        List<UserRole> userRoleList = userRoleRepository.findByEntityStatusNot(EntityStatus.DELETED);

        if(userRoleList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildUserRoleResponse(404, false, message, null,
                    null, null);
        }

        List<UserRoleDto> userRoleDtoList = modelMapper.map(userRoleList, new TypeToken<List<UserRoleDto>>(){}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserRoleResponse(200, true, message, null, userRoleDtoList,
                null);
    }

    @Override
    public UserRoleResponse update(EditUserRoleRequest editUserRoleRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = userRoleServiceValidator.isRequestValidForEditing(editUserRoleRequest, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_ROLE_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildUserRoleResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<UserRole> userRoleRetrieved = userRoleRepository.findByIdAndEntityStatusNot(editUserRoleRequest.getId(),
                EntityStatus.DELETED);

        if (userRoleRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ADDRESS_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserRoleResponse(400, false, message, null, null,
                    null);
        }

        UserRole userRoleToBeEdited = userRoleRetrieved.get();

        UserRole userRoleEdited = userRoleServiceAuditable.update(userRoleToBeEdited, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserRoleDto userRoleDtoReturned = modelMapper.map(userRoleEdited, UserRoleDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserRoleResponse(201, true, message, userRoleDtoReturned, null,
                null);
    }

    @Override
    public UserRoleResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userRoleServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildUserRoleResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<UserRole> userRoleRetrieved = userRoleRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userRoleRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserRoleResponse(404, false, message, null, null,
                    null);
        }

        UserRole userRoleToBeDeleted = userRoleRetrieved.get();
        userRoleToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        UserRole userRoleDeleted = userRoleServiceAuditable.delete(userRoleToBeDeleted, locale);

        UserRoleDto useRoleDtoReturned = modelMapper.map(userRoleDeleted, UserRoleDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserRoleResponse(200, true, message, useRoleDtoReturned, null,
                null);
    }

    @Override
    public UserRoleResponse findByMultipleFilters(UserRoleMultipleFiltersRequest userRoleMultipleFiltersRequest, String username, Locale locale) {

        String message = "";

        Specification<UserRole> spec = null;
        spec = addToSpec(spec, UserRoleSpecification::deleted);

        ValidatorDto validatorDto = userRoleServiceValidator.isRequestValidToRetrieveUserRoleByMultipleFilters(
                userRoleMultipleFiltersRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildUserRoleResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(userRoleMultipleFiltersRequest.getPage(),
                userRoleMultipleFiltersRequest.getSize());

        ValidatorDto roleValidatorDto = userRoleServiceValidator.isStringValid(
                userRoleMultipleFiltersRequest.getRole(), locale);

        if (roleValidatorDto.getSuccess()) {

            spec = addToSpec(userRoleMultipleFiltersRequest.getRole(), spec,
                    UserRoleSpecification::roleLike);
        }

        ValidatorDto descriptionValidatorDto = userRoleServiceValidator.isStringValid(
                userRoleMultipleFiltersRequest.getDescription(), locale);

        if (descriptionValidatorDto.getSuccess()) {

            spec = addToSpec(userRoleMultipleFiltersRequest.getDescription(), spec,
                    UserRoleSpecification::descriptionLike);
        }

        ValidatorDto searchValueValidatorDto = userRoleServiceValidator.isStringValid(
                userRoleMultipleFiltersRequest.getSearchValue(), locale);

        if (searchValueValidatorDto.getSuccess()) {

            spec = addToSpec(userRoleMultipleFiltersRequest.getSearchValue(), spec, UserRoleSpecification::any);
        }

        Page<UserRole> result = userRoleRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserRoleResponse(404, false, message,null, null,
                    null);
        }

        Page<UserRoleDto> userRoleDtoPage = convertUserRoleEntityToUserRoleDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_USER_ROLE_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserRoleResponse(200, true, message,null, null,
                userRoleDtoPage);
    }

    private Specification<UserRole> addToSpec(Specification<UserRole> spec,
                                               Function<EntityStatus, Specification<UserRole>> predicateMethod) {
        Specification<UserRole> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<UserRole> addToSpec(final String aString, Specification<UserRole> spec, Function<String,
            Specification<UserRole>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<UserRole> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    @Override
    public byte[] exportToCsv(List<UserRoleDto> userRoles) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (UserRoleDto userRole : userRoles) {
            sb.append(userRole.getId()).append(",")
                    .append(safe(userRole.getRole())).append(",")
                    .append(safe(userRole.getDescription())).append(",")
                    .append(userRole.getCreatedAt()).append(",")
                    .append(userRole.getUpdatedAt()).append(",")
                    .append(userRole.getEntityStatus()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<UserRoleDto> userRoles) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("User Roles");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (UserRoleDto userRole : userRoles) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(userRole.getId());
            row.createCell(1).setCellValue(safe(userRole.getRole()));
            row.createCell(2).setCellValue(safe(userRole.getDescription()));
            row.createCell(3).setCellValue(userRole.getCreatedAt() != null ? userRole.getCreatedAt().toString() : "");
            row.createCell(4).setCellValue(userRole.getUpdatedAt() != null ? userRole.getUpdatedAt().toString() : "");
            row.createCell(5).setCellValue(userRole.getEntityStatus() != null ? userRole.getEntityStatus().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<UserRoleDto> userRoles) throws DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("USER ROLE EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (UserRoleDto userRole : userRoles) {
            table.addCell(String.valueOf(userRole.getId()));
            table.addCell(safe(userRole.getRole()));
            table.addCell(safe(userRole.getDescription()));
            table.addCell(userRole.getCreatedAt() != null ? userRole.getCreatedAt().toString() : "");
            table.addCell(userRole.getUpdatedAt() != null ? userRole.getUpdatedAt().toString() : "");
            table.addCell(userRole.getEntityStatus() != null ? userRole.getEntityStatus().toString() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private Page<UserRoleDto> convertUserRoleEntityToUserRoleDto(Page<UserRole> userRolePage) {

        List<UserRole> userRoleList = userRolePage.getContent();
        List<UserRoleDto> userRoleDtoList = new ArrayList<>();

        for (UserRole userRole : userRolePage) {
            UserRoleDto userRoleDto = modelMapper.map(userRole, UserRoleDto.class);
            userRoleDtoList.add(userRoleDto);
        }

        int page = userRolePage.getNumber();
        int size = userRolePage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableUserRoles = PageRequest.of(page, size);

        return new PageImpl<UserRoleDto>(userRoleDtoList, pageableUserRoles, userRolePage.getTotalElements());
    }

    private UserRoleResponse buildUserRoleResponse(int statusCode, boolean isSuccess, String message,
                                                     UserRoleDto userRoleDto, List<UserRoleDto> userRoleDtoList,
                                                     Page<UserRoleDto> userRoleDtoPage) {

        UserRoleResponse userRoleResponse = new UserRoleResponse();

        userRoleResponse.setStatusCode(statusCode);
        userRoleResponse.setSuccess(isSuccess);
        userRoleResponse.setMessage(message);
        userRoleResponse.setUserRoleDto(userRoleDto);
        userRoleResponse.setUserRoleDtoList(userRoleDtoList);
        userRoleResponse.setUserRoleDtoPage(userRoleDtoPage);

        return userRoleResponse;
    }

    private UserRoleResponse buildUserRoleResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                     UserRoleDto userRoleDto, List<UserRoleDto> userRoleDtoList,
                                                     List<String> errorMessages) {

        UserRoleResponse userRoleResponse = new UserRoleResponse();

        userRoleResponse.setStatusCode(statusCode);
        userRoleResponse.setSuccess(isSuccess);
        userRoleResponse.setMessage(message);
        userRoleResponse.setUserRoleDto(userRoleDto);
        userRoleResponse.setUserRoleDtoList(userRoleDtoList);
        userRoleResponse.setErrorMessages(errorMessages);

        return userRoleResponse;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public ImportSummary importUserRolesFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                     .withSkipLines(1) // Skip header line
                     .build()) {

            // Read all records
            List<String[]> allRecords = csvReader.readAll();
            total = allRecords.size();

            // Get header row to map column indices
            Map<String, Integer> headerMap = new HashMap<>();
            String[] headers = {"ROLE", "DESCRIPTION"};

            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i], i);
            }

            int rowNumber = 1; // Start from row 1 (after header)
            for (String[] record : allRecords) {
                rowNumber++;
                try {
                    CreateUserRoleRequest request = new CreateUserRoleRequest();
                    request.setRole(record[headerMap.get("ROLE")]);
                    request.setDescription(record[headerMap.get("DESCRIPTION")]);

                    UserRoleResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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
            }
        } catch (CsvException e) {
            throw new IOException("Error processing CSV: " + e.getMessage(), e);
        }

        // Determine status code and success flag based on import results
        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0 
            ? "Import completed successfully. " + success + " out of " + total + " user roles imported."
            : "Import failed. No user roles were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

}
