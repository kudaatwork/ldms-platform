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
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.user.management.business.auditable.api.UserTypeServiceAuditable;
import projectlx.user.management.business.logic.api.UserTypeService;
import projectlx.user.management.business.validator.api.UserTypeServiceValidator;
import projectlx.user.management.model.EntityStatus;
import projectlx.user.management.model.UserType;
import projectlx.user.management.repository.UserRepository;
import projectlx.user.management.repository.UserTypeRepository;
import projectlx.user.management.repository.specification.UserTypeSpecification;
import projectlx.user.management.utils.dtos.ImportSummary;
import projectlx.user.management.utils.dtos.UserTypeDto;
import projectlx.user.management.utils.enums.I18Code;
import projectlx.user.management.utils.requests.CreateUserTypeRequest;
import projectlx.user.management.utils.requests.EditUserTypeRequest;
import projectlx.user.management.utils.requests.UserTypeMultipleFiltersRequest;
import projectlx.user.management.utils.responses.UserTypeResponse;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
public class UserTypeServiceImpl implements UserTypeService {

    private final UserTypeServiceValidator userTypeServiceValidator;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final UserTypeRepository userTypeRepository;
    private final UserRepository userRepository;
    private final UserTypeServiceAuditable userTypeServiceAuditable;

    private static final String[] HEADERS = {
            "ID", "USER TYPE NAME", "DESCRIPTION"
    };

    @Override
    public UserTypeResponse create(CreateUserTypeRequest createUserTypeRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userTypeServiceValidator.isCreateUserTypeRequestValid(createUserTypeRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_USER_TYPE_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildUserTypeResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserType userTypeToBeSaved = modelMapper.map(createUserTypeRequest, UserType.class);

        UserType userTypeSaved = userTypeServiceAuditable.create(userTypeToBeSaved, locale,
                username);

        UserTypeDto userTypeDtoReturned = modelMapper.map(userTypeSaved, UserTypeDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserTypeResponse(201, true, message, userTypeDtoReturned, null,
                null);
    }

    @Override
    public UserTypeResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userTypeServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildUserTypeResponseWithErrors(400, false, message, null, null, 
                    validatorDto.getErrorMessages());
        }

        Optional<UserType> userTypeRetrieved = userTypeRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userTypeRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserTypeResponse(404, false, message, null, null,
                    null);
        }

        UserType userTypeReturned = userTypeRetrieved.get();
        UserTypeDto userTypeDto = modelMapper.map(userTypeReturned, UserTypeDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserTypeResponse(200, true, message, userTypeDto, null,
                null);
    }

    @Override
    public UserTypeResponse findAllAsList(String username, Locale locale) {

        String message = "";

        List<UserType> userTypeList = userTypeRepository.findByEntityStatusNot(EntityStatus.DELETED);

        if(userTypeList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildUserTypeResponse(404, false, message, null,
                    null, null);
        }

        List<UserTypeDto> userTypeDtoList = modelMapper.map(userTypeList, new TypeToken<List<UserTypeDto>>(){}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserTypeResponse(200, true, message, null, userTypeDtoList,
                null);
    }

    @Override
    public UserTypeResponse update(EditUserTypeRequest editUserTypeRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = userTypeServiceValidator.isRequestValidForEditing(editUserTypeRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_USER_TYPE_INVALID_REQUEST.getCode(), new String[]{}, locale);

            return buildUserTypeResponseWithErrors(400, false, message, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<UserType> userTypeRetrieved = userTypeRepository.findByIdAndEntityStatusNot(editUserTypeRequest.getId(),
                EntityStatus.DELETED);

        if (userTypeRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildUserTypeResponse(400, false, message, null,
                    null, null);
        }

        UserType userTypeToBeEdited = userTypeRetrieved.get();

        // Check if other users are using this UserType
        long associatedUsersCount = userTypeToBeEdited.getUsers().size();

        UserType userTypeToSave;

        if (associatedUsersCount > 1) {

            // Clone the user type for this user (preserve immutability for others)
            userTypeToSave = new UserType();
            userTypeToSave.setUserTypeName(editUserTypeRequest.getUserTypeName());
            userTypeToSave.setDescription(editUserTypeRequest.getDescription());
            userTypeToSave.setEntityStatus(EntityStatus.ACTIVE);
            userTypeToSave.setCreatedAt(LocalDateTime.now());

        } else {
            // Safe to update directly
            applyUpdatesToUserType(userTypeToBeEdited, editUserTypeRequest);
            userTypeToSave = userTypeToBeEdited;
        }

        UserType userTypeEdited = userTypeServiceAuditable.update(userTypeToSave, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserTypeDto userTypeDtoReturned = modelMapper.map(userTypeEdited, UserTypeDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);

        return buildUserTypeResponse(201, true, message, userTypeDtoReturned, null,
                null);
    }

    @Override
    public UserTypeResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = userTypeServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildUserTypeResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Optional<UserType> userTypeRetrieved = userTypeRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (userTypeRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildUserTypeResponse(404, false, message, null, null,
                    null);
        }

        UserType userTypeToBeDeleted = userTypeRetrieved.get();
        userTypeToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        UserType userTypeDeleted = userTypeServiceAuditable.delete(userTypeToBeDeleted, locale, username);

        UserTypeDto useTypeDtoReturned = modelMapper.map(userTypeDeleted, UserTypeDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildUserTypeResponse(200, true, message, useTypeDtoReturned, null,
                null);
    }

    @Override
    public UserTypeResponse findByMultipleFilters(UserTypeMultipleFiltersRequest userTypeMultipleFiltersRequest, String username, Locale locale) {
        String message = "";

        Specification<UserType> spec = null;
        spec = addToSpec(spec, UserTypeSpecification::deleted);

        ValidatorDto validatorDto = userTypeServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(
                userTypeMultipleFiltersRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildUserTypeResponseWithErrors(400, false, message, null, null,
                    validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(userTypeMultipleFiltersRequest.getPage(),
                userTypeMultipleFiltersRequest.getSize());

        ValidatorDto userTypeNameValidatorDto = userTypeServiceValidator.isStringValid(
                userTypeMultipleFiltersRequest.getUserTypeName(), locale);

        if (userTypeNameValidatorDto.getSuccess()) {

            spec = addToSpec(userTypeMultipleFiltersRequest.getUserTypeName(), spec,
                    UserTypeSpecification::userTypeNameLike);
        }

        ValidatorDto descriptionValidatorDto = userTypeServiceValidator.isStringValid(
                userTypeMultipleFiltersRequest.getDescription(), locale);

        if (descriptionValidatorDto.getSuccess()) {

            spec = addToSpec(userTypeMultipleFiltersRequest.getDescription(), spec,
                    UserTypeSpecification::descriptionLike);
        }

        ValidatorDto searchValueValidatorDto = userTypeServiceValidator.isStringValid(
                userTypeMultipleFiltersRequest.getSearchValue(), locale);

        if (searchValueValidatorDto.getSuccess()) {

            spec = addToSpec(userTypeMultipleFiltersRequest.getSearchValue(), spec, UserTypeSpecification::any);
        }

        Page<UserType> result = userTypeRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildUserTypeResponse(404, false, message,null, null,
                    null);
        }

        Page<UserTypeDto> userTypeDtoPage = convertUserTypeEntityToUserTypeDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_USER_TYPE_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildUserTypeResponse(200, true, message,null, null,
                userTypeDtoPage);
    }

    private Specification<UserType> addToSpec(Specification<UserType> spec,
                                              Function<EntityStatus, Specification<UserType>> predicateMethod) {
        Specification<UserType> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<UserType> addToSpec(final String aString, Specification<UserType> spec, Function<String,
            Specification<UserType>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<UserType> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Page<UserTypeDto> convertUserTypeEntityToUserTypeDto(Page<UserType> userTypePage) {

        List<UserType> userTypeList = userTypePage.getContent();
        List<UserTypeDto> userTypeDtoList = new ArrayList<>();

        for (UserType userType : userTypePage) {
            UserTypeDto userTypeDto = modelMapper.map(userType, UserTypeDto.class);
            userTypeDtoList.add(userTypeDto);
        }

        int page = userTypePage.getNumber();
        int size = userTypePage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableUserTypes = PageRequest.of(page, size);

        return new PageImpl<UserTypeDto>(userTypeDtoList, pageableUserTypes, userTypePage.getTotalElements());
    }

    private void applyUpdatesToUserType(UserType userType, EditUserTypeRequest editRequest) {
        userType.setUserTypeName(editRequest.getUserTypeName());
        userType.setDescription(editRequest.getDescription());
    }

    private UserTypeResponse buildUserTypeResponse(int statusCode, boolean isSuccess, String message,
                                                   UserTypeDto userTypeDto, List<UserTypeDto> userTypeDtoList,
                                                   Page<UserTypeDto> userTypeDtoPage) {

        UserTypeResponse userTypeResponse = new UserTypeResponse();

        userTypeResponse.setStatusCode(statusCode);
        userTypeResponse.setSuccess(isSuccess);
        userTypeResponse.setMessage(message);
        userTypeResponse.setUserTypeDto(userTypeDto);
        userTypeResponse.setUserTypeDtoList(userTypeDtoList);
        userTypeResponse.setUserTypeDtoPage(userTypeDtoPage);

        return userTypeResponse;
    }

    private UserTypeResponse buildUserTypeResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                     UserTypeDto userTypeDto, List<UserTypeDto> userTypeDtoList,
                                                     List<String> errorMessages) {

        UserTypeResponse userTypeResponse = new UserTypeResponse();

        userTypeResponse.setStatusCode(statusCode);
        userTypeResponse.setSuccess(isSuccess);
        userTypeResponse.setMessage(message);
        userTypeResponse.setUserTypeDto(userTypeDto);
        userTypeResponse.setUserTypeDtoList(userTypeDtoList);
        userTypeResponse.setErrorMessages(errorMessages);

        return userTypeResponse;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<UserTypeDto> userTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (UserTypeDto userType : userTypes) {
            sb.append(userType.getId()).append(",")
                    .append(safe(userType.getUserTypeName())).append(",")
                    .append(safe(userType.getDescription())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<UserTypeDto> userTypes) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("User Types");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (UserTypeDto userType : userTypes) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(userType.getId());
            row.createCell(1).setCellValue(safe(userType.getUserTypeName()));
            row.createCell(2).setCellValue(safe(userType.getDescription()));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<UserTypeDto> userTypes) throws DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("USER TYPE EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (UserTypeDto userType : userTypes) {
            table.addCell(String.valueOf(userType.getId()));
            table.addCell(safe(userType.getUserTypeName()));
            table.addCell(safe(userType.getDescription()));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public ImportSummary importUserTypesFromCsv(InputStream csvInputStream) throws IOException {
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
            String[] headers = {"USER TYPE NAME", "DESCRIPTION"};

            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i], i);
            }

            int rowNumber = 1; // Start from row 1 (after header)
            for (String[] record : allRecords) {
                rowNumber++;
                try {
                    CreateUserTypeRequest request = new CreateUserTypeRequest();
                    request.setUserTypeName(record[headerMap.get("USER TYPE NAME")]);
                    request.setDescription(record[headerMap.get("DESCRIPTION")]);

                    UserTypeResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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
            ? "Import completed successfully. " + success + " out of " + total + " user types imported."
            : "Import failed. No user types were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }
}
