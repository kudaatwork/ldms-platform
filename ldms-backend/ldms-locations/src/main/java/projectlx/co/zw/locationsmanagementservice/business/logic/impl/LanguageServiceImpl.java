package projectlx.co.zw.locationsmanagementservice.business.logic.impl;

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
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.LanguageServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LanguageService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.LanguageServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.Language;
import projectlx.co.zw.locationsmanagementservice.repository.LanguageRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.LanguageSpecification;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LanguageCsvDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LanguageDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LanguageMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLanguageRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LanguageResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.globalvalidators.Validators;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
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
import java.util.stream.Collectors;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;

@RequiredArgsConstructor
public class LanguageServiceImpl implements LanguageService {

    private final LanguageServiceValidator languageServiceValidator;
    private final LanguageRepository languageRepository;
    private final LanguageServiceAuditable languageServiceAuditable;
    private final MessageService messageService;
    private final ModelMapper modelMapper;

    private static final String[] HEADERS = {
            "ID", "NAME", "ISO CODE", "NATIVE NAME", "IS DEFAULT", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
    };

    private static final String[] CSV_HEADERS = {
            "NAME", "ISO CODE", "NATIVE NAME", "IS DEFAULT"
    };

    @Override
    public LanguageResponse create(CreateLanguageRequest createLanguageRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = languageServiceValidator.isCreateLanguageRequestValid(createLanguageRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_CREATE_LANGUAGE_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildLanguageResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        String normalizedName = Validators.capitalizeWords(createLanguageRequest.getName());
        Optional<Language> languageRetrieved =
                languageRepository.findByNameAndEntityStatusNot(normalizedName, EntityStatus.DELETED);

        if (languageRetrieved.isPresent()) {

            message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_ALREADY_EXISTS.getCode(), new String[]{},
                    locale);

            return buildLanguageResponse(400, false, message, null,
                    null, null);
        }

        Optional<Language> deletedLanguage = languageRepository.findByName(normalizedName)
                .filter(language -> language.getEntityStatus() == EntityStatus.DELETED);

        createLanguageRequest.setName(normalizedName);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Language languageSaved;
        if (deletedLanguage.isPresent()) {
            Language languageToBeReactivated = deletedLanguage.get();
            languageToBeReactivated.setName(normalizedName);
            languageToBeReactivated.setIsoCode(createLanguageRequest.getIsoCode());
            languageToBeReactivated.setNativeName(createLanguageRequest.getNativeName());
            languageToBeReactivated.setIsDefault(createLanguageRequest.getIsDefault());
            languageToBeReactivated.setEntityStatus(EntityStatus.ACTIVE);
            languageSaved = languageServiceAuditable.update(languageToBeReactivated, locale, username);
        } else {
            Language languageToBeSaved = modelMapper.map(createLanguageRequest, Language.class);
            languageSaved = languageServiceAuditable.create(languageToBeSaved, locale, username);
        }

        LanguageDto languageDtoReturned = modelMapper.map(languageSaved, LanguageDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_CREATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildLanguageResponse(201, true, message, languageDtoReturned, null,
                null);
    }

    @Override
    public LanguageResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = languageServiceValidator.isIdValid(id, locale);

        if(!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]
                    {}, locale);

            return buildLanguageResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Language> languageRetrieved = languageRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (languageRetrieved.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildLanguageResponse(404, false, message, null, null,
                    null);
        }

        Language languageReturned = languageRetrieved.get();
        LanguageDto languageDto = modelMapper.map(languageReturned, LanguageDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildLanguageResponse(200, true, message, languageDto, null,
                null);
    }

    @Override
    public LanguageResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<Language> languageList = languageRepository.findAll().stream()
                .filter(language -> language.getEntityStatus() != EntityStatus.DELETED)
                .collect(Collectors.toList());

        if(languageList.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_NOT_FOUND.getCode(), new String[]
                    {}, locale);

            return buildLanguageResponse(404, false, message, null,
                    null, null);
        }

        List<LanguageDto> languageDtoList = modelMapper.map(languageList, new TypeToken<List<LanguageDto>>(){}.getType());

        message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildLanguageResponse(200, true, message, null, languageDtoList,
                null);
    }

    @Override
    public LanguageResponse update(EditLanguageRequest editLanguageRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = languageServiceValidator.isRequestValidForEditing(editLanguageRequest, locale);

        if(!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_LANGUAGE_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildLanguageResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Language> languageRetrieved = languageRepository.findByIdAndEntityStatusNot(editLanguageRequest.getId(),
                EntityStatus.DELETED);

        if (languageRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildLanguageResponse(404, false, message, null, null,
                    null);
        }

        Language languageToBeEdited = languageRetrieved.get();

        // Update language properties from request
        languageToBeEdited.setName(Validators.capitalizeWords(editLanguageRequest.getName()));
        languageToBeEdited.setIsoCode(editLanguageRequest.getIsoCode());
        languageToBeEdited.setNativeName(editLanguageRequest.getNativeName());
        languageToBeEdited.setIsDefault(editLanguageRequest.getIsDefault());

        Language languageEdited = languageServiceAuditable.update(languageToBeEdited, locale, username);

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        LanguageDto languageDtoReturned = modelMapper.map(languageEdited, LanguageDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_UPDATED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildLanguageResponse(200, true, message, languageDtoReturned, null,
                null);
    }

    @Override
    public LanguageResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = languageServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{},
                    locale);

            return buildLanguageResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<Language> languageRetrieved = languageRepository.findByIdAndEntityStatusNot(id,
                EntityStatus.DELETED);

        if (languageRetrieved.isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_NOT_FOUND.getCode(), new String[]{},
                    locale);

            return buildLanguageResponse(404, false, message, null, null,
                    null);
        }

        Language languageToBeDeleted = languageRetrieved.get();
        languageToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        Language languageDeleted = languageServiceAuditable.delete(languageToBeDeleted, locale);

        LanguageDto languageDtoReturned = modelMapper.map(languageDeleted, LanguageDto.class);

        message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_DELETED_SUCCESSFULLY.getCode(), new String[]{},
                locale);

        return buildLanguageResponse(200, true, message, languageDtoReturned, null,
                null);
    }

    @Override
    public LanguageResponse findByMultipleFilters(LanguageMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";

        Specification<Language> spec = null;
        spec = addToSpec(spec, LanguageSpecification::deleted);

        ValidatorDto validatorDto = languageServiceValidator.isRequestValidToRetrieveUsersByMultipleFilters(
                request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_LANGUAGE_INVALID_REQUEST.getCode(),
                    new String[]{}, locale);

            return buildLanguageResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (isNotEmpty(request.getName())) {
            spec = addToSpec(request.getName(), spec, LanguageSpecification::nameLike);
        }

        if (isNotEmpty(request.getIsoCode())) {
            spec = addToSpec(request.getIsoCode(), spec, LanguageSpecification::isoCodeLike);
        }

        if (isNotEmpty(request.getNativeName())) {
            spec = addToSpec(request.getNativeName(), spec, LanguageSpecification::nativeNameLike);
        }

        if (isNotEmpty(request.getSearchValue())) {
            spec = addToSpec(request.getSearchValue(), spec, LanguageSpecification::any);
        }

        if (request.getEntityStatus() != null) {
            spec = addToSpec(request.getEntityStatus(), spec, LanguageSpecification::hasEntityStatus);
        }

        Page<Language> result = languageRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {

            message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_NOT_FOUND.getCode(),
                    new String[]{}, locale);

            return buildLanguageResponse(404, false, message, null, null,
                    null);
        }

        Page<LanguageDto> languageDtoPage = convertLanguageEntityToLanguageDto(result);

        message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_RETRIEVED_SUCCESSFULLY.getCode(),
                new String[]{}, locale);

        return buildLanguageResponse(200, true, message, null, null,
                languageDtoPage);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<LanguageDto> items) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (LanguageDto language : items) {
            sb.append(language.getId()).append(",")
                    .append(safe(language.getName())).append(",")
                    .append(safe(language.getIsoCode())).append(",")
                    .append(safe(language.getNativeName())).append(",")
                    .append(language.getIsDefault()).append(",")
                    .append(language.getCreatedAt()).append(",")
                    .append(language.getUpdatedAt()).append(",")
                    .append(language.getEntityStatus()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<LanguageDto> items) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Languages");

        Row header = sheet.createRow(0);

        for (int i = 0; i < HEADERS.length; i++) {
            header.createCell(i).setCellValue(HEADERS[i]);
        }

        int rowIdx = 1;

        for (LanguageDto language : items) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(language.getId());
            row.createCell(1).setCellValue(safe(language.getName()));
            row.createCell(2).setCellValue(safe(language.getIsoCode()));
            row.createCell(3).setCellValue(safe(language.getNativeName()));
            row.createCell(4).setCellValue(language.getIsDefault() != null ? language.getIsDefault().toString() : "false");
            row.createCell(5).setCellValue(language.getCreatedAt() != null ? language.getCreatedAt().toString() : "");
            row.createCell(6).setCellValue(language.getUpdatedAt() != null ? language.getUpdatedAt().toString() : "");
            row.createCell(7).setCellValue(language.getEntityStatus() != null ? language.getEntityStatus().toString() : "");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    @Override
    public byte[] exportToPdf(List<LanguageDto> items) throws DocumentException {

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        document.add(new Paragraph("LANGUAGE EXPORT", font));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(HEADERS.length);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        for (LanguageDto language : items) {
            table.addCell(String.valueOf(language.getId()));
            table.addCell(safe(language.getName()));
            table.addCell(safe(language.getIsoCode()));
            table.addCell(safe(language.getNativeName()));
            table.addCell(language.getIsDefault() != null ? language.getIsDefault().toString() : "false");
            table.addCell(language.getCreatedAt() != null ? language.getCreatedAt().toString() : "");
            table.addCell(language.getUpdatedAt() != null ? language.getUpdatedAt().toString() : "");
            table.addCell(language.getEntityStatus() != null ? language.getEntityStatus().toString() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    @Override
    public ImportSummary importLanguageFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8)) {
            HeaderColumnNameMappingStrategy<LanguageCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(LanguageCsvDto.class);

            CsvToBean<LanguageCsvDto> csvToBean = new CsvToBeanBuilder<LanguageCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<LanguageCsvDto> rows = csvToBean.parse();
            total = rows.size();

            int rowNum = 1;
            for (LanguageCsvDto row : rows) {
                rowNum++;
                try {
                    if (row.getName() == null || row.getName().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing language name");
                        continue;
                    }

                    CreateLanguageRequest request = new CreateLanguageRequest();
                    request.setName(row.getName());
                    request.setIsoCode(row.getIsoCode());
                    request.setNativeName(row.getNativeName());
                    if (row.getIsDefault() != null) {
                        request.setIsDefault(Boolean.parseBoolean(row.getIsDefault()));
                    }

                    LanguageResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

                    if (response.isSuccess()) {
                        success++;
                    } else {
                        failed++;
                        errors.add("Row " + rowNum + ": " + response.getMessage());
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + rowNum + ": Unexpected error - " + e.getMessage());
                }
            }
        }

        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0
                ? "Import completed successfully. " + success + " out of " + total + " languages imported."
                : "Import failed. No languages were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private Specification<Language> addToSpec(Specification<Language> spec,
                                           Function<EntityStatus, Specification<Language>> predicateMethod) {
        Specification<Language> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<Language> addToSpec(final String aString, Specification<Language> spec, Function<String,
            Specification<Language>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<Language> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<Language> addToSpec(final EntityStatus entityStatus, Specification<Language> spec, Function<EntityStatus,
            Specification<Language>> predicateMethod) {
        if (entityStatus != null) {
            Specification<Language> localSpec = Specification.where(predicateMethod.apply(entityStatus));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Page<LanguageDto> convertLanguageEntityToLanguageDto(Page<Language> languagePage) {
        List<Language> languageList = languagePage.getContent();
        List<LanguageDto> languageDtoList = new ArrayList<>();

        for (Language language : languagePage) {
            LanguageDto languageDto = modelMapper.map(language, LanguageDto.class);
            languageDtoList.add(languageDto);
        }

        int page = languagePage.getNumber();
        int size = languagePage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableLanguages = PageRequest.of(page, size);

        return new PageImpl<>(languageDtoList, pageableLanguages, languagePage.getTotalElements());
    }

    private LanguageResponse buildLanguageResponse(int statusCode, boolean isSuccess, String message,
                                                 LanguageDto languageDto, List<LanguageDto> languageDtoList,
                                                     Page<LanguageDto> languageDtoPage) {

        LanguageResponse languageResponse = new LanguageResponse();

        languageResponse.setStatusCode(statusCode);
        languageResponse.setSuccess(isSuccess);
        languageResponse.setMessage(message);
        languageResponse.setLanguageDto(languageDto);
        languageResponse.setLanguageDtoList(languageDtoList);
        languageResponse.setLanguageDtoPage(languageDtoPage);

        return languageResponse;
    }

    private LanguageResponse buildLanguageResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                         LanguageDto languageDto, List<LanguageDto> languageDtoList,
                                                         Page<LanguageDto> languageDtoPage,List<String> errorMessages) {

        LanguageResponse languageResponse = new LanguageResponse();

        languageResponse.setStatusCode(statusCode);
        languageResponse.setSuccess(isSuccess);
        languageResponse.setMessage(message);
        languageResponse.setLanguageDto(languageDto);
        languageResponse.setLanguageDtoList(languageDtoList);
        languageResponse.setLanguageDtoPage(languageDtoPage);
        languageResponse.setErrorMessages(errorMessages);

        return languageResponse;
    }
}
