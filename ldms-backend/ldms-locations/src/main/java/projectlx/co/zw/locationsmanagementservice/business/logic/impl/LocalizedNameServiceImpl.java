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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.LocalizedNameServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocalizedNameService;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.LocalizedNameServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.Language;
import projectlx.co.zw.locationsmanagementservice.model.LocalizedName;
import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.locationsmanagementservice.repository.CountryRepository;
import projectlx.co.zw.locationsmanagementservice.repository.DistrictRepository;
import projectlx.co.zw.locationsmanagementservice.repository.LanguageRepository;
import projectlx.co.zw.locationsmanagementservice.repository.LocalizedNameRepository;
import projectlx.co.zw.locationsmanagementservice.repository.ProvinceRepository;
import projectlx.co.zw.locationsmanagementservice.repository.SuburbRepository;
import projectlx.co.zw.locationsmanagementservice.repository.specification.LocalizedNameSpecification;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.ImportSummary;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocalizedNameCsvDto;
import projectlx.co.zw.locationsmanagementservice.utils.dtos.LocalizedNameDto;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocalizedNameRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocalizedNameMultipleFiltersRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocalizedNameResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
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
public class LocalizedNameServiceImpl implements LocalizedNameService {

    private final LocalizedNameServiceValidator localizedNameServiceValidator;
    private final LocalizedNameRepository localizedNameRepository;
    private final LanguageRepository languageRepository;
    private final CountryRepository countryRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final SuburbRepository suburbRepository;
    private final LocalizedNameServiceAuditable localizedNameServiceAuditable;
    private final MessageService messageService;
    private final ModelMapper modelMapper;

    private static final String[] HEADERS = {
            "ID", "VALUE", "LANGUAGE", "REFERENCE TYPE", "REFERENCE ID", "CREATED AT", "UPDATED AT", "ENTITY STATUS"
    };

    /**
     * Entity stores language + exactly one geography reference via relations; {@link LocalizedNameDto} exposes
     * flattened {@code languageId}, {@code referenceType}, {@code referenceId}. ModelMapper does not derive these.
     */
    private LocalizedNameDto toLocalizedNameDto(LocalizedName source) {
        LocalizedNameDto dto = modelMapper.map(source, LocalizedNameDto.class);
        if (source.getLanguage() != null) {
            dto.setLanguageId(source.getLanguage().getId());
        }
        if (source.getSuburb() != null) {
            dto.setReferenceType("SUBURB");
            dto.setReferenceId(source.getSuburb().getId());
        } else if (source.getDistrict() != null) {
            dto.setReferenceType("DISTRICT");
            dto.setReferenceId(source.getDistrict().getId());
        } else if (source.getProvince() != null) {
            dto.setReferenceType("PROVINCE");
            dto.setReferenceId(source.getProvince().getId());
        } else if (source.getCountry() != null) {
            dto.setReferenceType("COUNTRY");
            dto.setReferenceId(source.getCountry().getId());
        }
        return dto;
    }

    @Override
    @Transactional
    public LocalizedNameResponse create(CreateLocalizedNameRequest request, Locale locale, String username) {

        String message;

        ValidatorDto validatorDto = localizedNameServiceValidator.isCreateLocalizedNameRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_LOCALIZED_NAME_INVALID_REQUEST.getCode(), new String[]{},
                    locale);

            return buildLocalizedNameResponseWithErrors(400, false, message, null,
                    null, null, validatorDto.getErrorMessages());
        }

        Optional<Language> languageOptional = languageRepository.findByIdAndEntityStatusNot(request.getLanguageId(),
                EntityStatus.DELETED);

        if (languageOptional.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_LANGUAGE_NOT_FOUND.getCode(), new String[]{},
                    locale);
            return buildLocalizedNameResponse(404, false, message, null,
                    null, null);
        }

        LocalizedName localizedNameToBeSaved = new LocalizedName();
        localizedNameToBeSaved.setValue(request.getValue());
        localizedNameToBeSaved.setLanguage(languageOptional.get());

        if (request.getReferenceType() != null && request.getReferenceId() != null) {
            boolean referenceFound = false;
            switch (request.getReferenceType().toUpperCase()) {
                case "COUNTRY":
                    Optional<Country> countryOpt = countryRepository.findByIdAndEntityStatusNot(request.getReferenceId(),
                            EntityStatus.DELETED);
                    if (countryOpt.isPresent()) {
                        localizedNameToBeSaved.setCountry(countryOpt.get());
                        referenceFound = true;
                    }
                    break;
                case "PROVINCE":
                    Optional<Province> provinceOpt = provinceRepository.findByIdAndEntityStatusNot(request.getReferenceId(),
                            EntityStatus.DELETED);
                    if (provinceOpt.isPresent()) {
                        localizedNameToBeSaved.setProvince(provinceOpt.get());
                        referenceFound = true;
                    }
                    break;
                case "DISTRICT":
                    Optional<District> districtOpt = districtRepository.findByIdAndEntityStatusNot(request.getReferenceId(),
                            EntityStatus.DELETED);
                    if (districtOpt.isPresent()) {
                        localizedNameToBeSaved.setDistrict(districtOpt.get());
                        referenceFound = true;
                    }
                    break;
                case "SUBURB":
                    Optional<Suburb> suburbOpt = suburbRepository.findByIdAndEntityStatusNot(request.getReferenceId(),
                            EntityStatus.DELETED);
                    if (suburbOpt.isPresent()) {
                        localizedNameToBeSaved.setSuburb(suburbOpt.get());
                        referenceFound = true;
                    }
                    break;
                default:
                    // Assuming a specific I18Code for invalid reference type
                    message = messageService.getMessage(I18Code.MESSAGE_CREATE_LOCALIZED_NAME_INVALID_REFERENCE_TYPE.getCode(),
                            new String[]{request.getReferenceType()}, locale);
                    return buildLocalizedNameResponse(400, false, message, null,
                            null, null);
            }

            if (!referenceFound) {
                // Assuming a specific I18Code for when the referenced entity ID is not found
                message = messageService.getMessage(I18Code.MESSAGE_REFERENCE_ENTITY_NOT_FOUND.getCode(),
                        new String[]{request.getReferenceType(), String.valueOf(request.getReferenceId())}, locale);
                return buildLocalizedNameResponse(404, false, message, null,
                        null, null);
            }
        } else {
            message = messageService.getMessage(I18Code.MESSAGE_CREATE_LOCALIZED_NAME_REFERENCE_TYPE_MISSING.getCode(),
                    new String[]{}, locale);
            return buildLocalizedNameResponse(400, false, message, null,
                    null, null);
        }

        LocalizedName localizedNameSaved = localizedNameServiceAuditable.create(localizedNameToBeSaved, locale, username);
        LocalizedNameDto localizedNameDtoReturned = toLocalizedNameDto(localizedNameSaved);
        message = messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_CREATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildLocalizedNameResponse(201, true, message, localizedNameDtoReturned, null,
                null);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalizedNameResponse findById(Long id, Locale locale, String username) {
        String message;
        ValidatorDto validatorDto = localizedNameServiceValidator.isIdValid(id, locale);
        if(!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildLocalizedNameResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }

        Optional<LocalizedName> localizedNameRetrieved = localizedNameRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (localizedNameRetrieved.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildLocalizedNameResponse(404, false, message, null, null, null);
        }

        LocalizedNameDto localizedNameDto = toLocalizedNameDto(localizedNameRetrieved.get());
        message = messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildLocalizedNameResponse(200, true, message, localizedNameDto, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalizedNameResponse findAllAsList(Locale locale, String username) {
        List<LocalizedName> localizedNameList = localizedNameRepository.findAll().stream()
                .filter(localizedName -> localizedName.getEntityStatus() != EntityStatus.DELETED)
                .collect(Collectors.toList());

        if(localizedNameList.isEmpty()) {
            String message = messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildLocalizedNameResponse(404, false, message, null, null, null);
        }

        List<LocalizedNameDto> localizedNameDtoList = localizedNameList.stream().map(this::toLocalizedNameDto).collect(Collectors.toList());
        String message = messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildLocalizedNameResponse(200, true, message, null, localizedNameDtoList, null);
    }

    @Override
    @Transactional
    public LocalizedNameResponse update(EditLocalizedNameRequest request, String username, Locale locale) {
        String message;
        ValidatorDto validatorDto = localizedNameServiceValidator.isRequestValidForEditing(request, locale);
        if(!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_LOCALIZED_NAME_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildLocalizedNameResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }

        Optional<LocalizedName> localizedNameRetrieved = localizedNameRepository.findByIdAndEntityStatusNot(request.getId(), EntityStatus.DELETED);
        if (localizedNameRetrieved.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildLocalizedNameResponse(404, false, message, null, null, null);
        }

        LocalizedName localizedNameToBeEdited = localizedNameRetrieved.get();
        localizedNameToBeEdited.setValue(request.getValue());

        if (request.getLanguageId() != null) {
            languageRepository.findByIdAndEntityStatusNot(request.getLanguageId(), EntityStatus.DELETED)
                    .ifPresent(localizedNameToBeEdited::setLanguage);
        }

        if (request.getReferenceType() != null && request.getReferenceId() != null) {
            boolean referenceFound = false;
            // Clear existing references to avoid orphaned links if a new reference is successfully found
            switch (request.getReferenceType().toUpperCase()) {
                case "COUNTRY":
                    Optional<Country> countryOpt = countryRepository.findByIdAndEntityStatusNot(request.getReferenceId(), EntityStatus.DELETED);
                    if (countryOpt.isPresent()) {
                        clearAllReferences(localizedNameToBeEdited);
                        localizedNameToBeEdited.setCountry(countryOpt.get());
                        referenceFound = true;
                    }
                    break;
                case "PROVINCE":
                    Optional<Province> provinceOpt = provinceRepository.findByIdAndEntityStatusNot(request.getReferenceId(), EntityStatus.DELETED);
                    if (provinceOpt.isPresent()) {
                        clearAllReferences(localizedNameToBeEdited);
                        localizedNameToBeEdited.setProvince(provinceOpt.get());
                        referenceFound = true;
                    }
                    break;
                case "DISTRICT":
                    Optional<District> districtOpt = districtRepository.findByIdAndEntityStatusNot(request.getReferenceId(), EntityStatus.DELETED);
                    if (districtOpt.isPresent()) {
                        clearAllReferences(localizedNameToBeEdited);
                        localizedNameToBeEdited.setDistrict(districtOpt.get());
                        referenceFound = true;
                    }
                    break;
                case "SUBURB":
                    Optional<Suburb> suburbOpt = suburbRepository.findByIdAndEntityStatusNot(request.getReferenceId(), EntityStatus.DELETED);
                    if (suburbOpt.isPresent()) {
                        clearAllReferences(localizedNameToBeEdited);
                        localizedNameToBeEdited.setSuburb(suburbOpt.get());
                        referenceFound = true;
                    }
                    break;
            }
            if (!referenceFound) {
                message = messageService.getMessage(I18Code.MESSAGE_REFERENCE_ENTITY_NOT_FOUND.getCode(), new String[]{request.getReferenceType(), String.valueOf(request.getReferenceId())}, locale);
                return buildLocalizedNameResponse(404, false, message, null, null, null);
            }
        }

        LocalizedName localizedNameEdited = localizedNameServiceAuditable.update(localizedNameToBeEdited, locale, username);
        LocalizedNameDto localizedNameDtoReturned = toLocalizedNameDto(localizedNameEdited);
        message = messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildLocalizedNameResponse(200, true, message, localizedNameDtoReturned, null, null);
    }

    private void clearAllReferences(LocalizedName localizedName) {
        localizedName.setCountry(null);
        localizedName.setProvince(null);
        localizedName.setDistrict(null);
        localizedName.setSuburb(null);
    }

    @Override
    @Transactional
    public LocalizedNameResponse delete(Long id, Locale locale, String username) {
        String message;
        ValidatorDto validatorDto = localizedNameServiceValidator.isIdValid(id, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale);
            return buildLocalizedNameResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }

        Optional<LocalizedName> localizedNameRetrieved = localizedNameRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (localizedNameRetrieved.isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildLocalizedNameResponse(404, false, message, null, null, null);
        }

        LocalizedName localizedNameToBeDeleted = localizedNameRetrieved.get();
        localizedNameToBeDeleted.setEntityStatus(EntityStatus.DELETED);
        LocalizedName localizedNameDeleted = localizedNameServiceAuditable.delete(localizedNameToBeDeleted, locale);
        LocalizedNameDto localizedNameDtoReturned = toLocalizedNameDto(localizedNameDeleted);
        message = messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_DELETED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildLocalizedNameResponse(200, true, message, localizedNameDtoReturned, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalizedNameResponse findByMultipleFilters(LocalizedNameMultipleFiltersRequest request, String username, Locale locale) {
        String message;
        ValidatorDto validatorDto = localizedNameServiceValidator.isRequestValidToRetrieveLocalizedNamesByMultipleFilters(request, locale);
        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_UPDATE_LOCALIZED_NAME_INVALID_REQUEST.getCode(), new String[]{}, locale);
            return buildLocalizedNameResponseWithErrors(400, false, message, null, null, null, validatorDto.getErrorMessages());
        }

        // Correctly use the 'deleted' method from your specification which acts as 'isNotDeleted'
        Specification<LocalizedName> spec = Specification.where(LocalizedNameSpecification.deleted(EntityStatus.DELETED));

        if (isNotEmpty(request.getValue())) {
            spec = spec.and(LocalizedNameSpecification.valueLike(request.getValue()));
        }
        if (request.getLanguageId() != null) {
            spec = spec.and(LocalizedNameSpecification.byLanguage(request.getLanguageId()));
        }
        if (isNotEmpty(request.getSearchValue())) {
            spec = spec.and(LocalizedNameSpecification.any(request.getSearchValue()));
        }
        if (request.getEntityStatus() != null) {
            spec = spec.and(LocalizedNameSpecification.hasEntityStatus(request.getEntityStatus()));
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Direction.ASC, "id"));
        Page<LocalizedName> result = localizedNameRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildLocalizedNameResponse(404, false, message, null, null, null);
        }

        Page<LocalizedNameDto> localizedNameDtoPage = result.map(this::toLocalizedNameDto);
        message = messageService.getMessage(I18Code.MESSAGE_LOCALIZED_NAME_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildLocalizedNameResponse(200, true, message, null, null, localizedNameDtoPage);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<LocalizedNameDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (LocalizedNameDto localizedName : items) {
            sb.append(localizedName.getId()).append(",")
                    .append(safe(localizedName.getValue())).append(",")
                    .append(localizedName.getLanguageId()).append(",")
                    .append(safe(localizedName.getReferenceType())).append(",")
                    .append(localizedName.getReferenceId()).append(",")
                    .append(localizedName.getCreatedAt()).append(",")
                    .append(localizedName.getUpdatedAt()).append(",")
                    .append(localizedName.getEntityStatus()).append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<LocalizedNameDto> items) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Localized Names");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int rowIdx = 1;
            for (LocalizedNameDto localizedName : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(localizedName.getId());
                row.createCell(1).setCellValue(safe(localizedName.getValue()));
                row.createCell(2).setCellValue(localizedName.getLanguageId());
                row.createCell(3).setCellValue(safe(localizedName.getReferenceType()));
                row.createCell(4).setCellValue(localizedName.getReferenceId());
                row.createCell(5).setCellValue(localizedName.getCreatedAt() != null ? localizedName.getCreatedAt().toString() : "");
                row.createCell(6).setCellValue(localizedName.getUpdatedAt() != null ? localizedName.getUpdatedAt().toString() : "");
                row.createCell(7).setCellValue(localizedName.getEntityStatus() != null ? localizedName.getEntityStatus().toString() : "");
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(List<LocalizedNameDto> items) throws DocumentException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();
            Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            document.add(new Paragraph("LOCALIZED NAME EXPORT", font));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(HEADERS.length);
            for (String header : HEADERS) {
                PdfPCell cell = new PdfPCell(new Phrase(header, font));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }
            for (LocalizedNameDto localizedName : items) {
                table.addCell(String.valueOf(localizedName.getId()));
                table.addCell(safe(localizedName.getValue()));
                table.addCell(String.valueOf(localizedName.getLanguageId()));
                table.addCell(safe(localizedName.getReferenceType()));
                table.addCell(String.valueOf(localizedName.getReferenceId()));
                table.addCell(localizedName.getCreatedAt() != null ? localizedName.getCreatedAt().toString() : "");
                table.addCell(localizedName.getUpdatedAt() != null ? localizedName.getUpdatedAt().toString() : "");
                table.addCell(localizedName.getEntityStatus() != null ? localizedName.getEntityStatus().toString() : "");
            }
            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ImportSummary importLocalizedNameFromCsv(InputStream csvInputStream) throws IOException {
        List<String> errors = new ArrayList<>();
        int success = 0;
        int failed = 0;
        int total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8)) {
            HeaderColumnNameMappingStrategy<LocalizedNameCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(LocalizedNameCsvDto.class);

            CsvToBean<LocalizedNameCsvDto> csvToBean = new CsvToBeanBuilder<LocalizedNameCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<LocalizedNameCsvDto> rows = csvToBean.parse();
            total = rows.size();

            int rowNum = 1;
            for (LocalizedNameCsvDto row : rows) {
                rowNum++;
                try {
                    if (row.getValue() == null || row.getValue().trim().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing VALUE");
                        continue;
                    }
                    if (row.getLanguageId() == null || row.getLanguageId().trim().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing LANGUAGE ID");
                        continue;
                    }
                    if (row.getReferenceType() == null || row.getReferenceType().trim().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing REFERENCE TYPE");
                        continue;
                    }
                    if (row.getReferenceId() == null || row.getReferenceId().trim().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing REFERENCE ID");
                        continue;
                    }
                    long languageId;
                    try {
                        languageId = Long.parseLong(row.getLanguageId().trim());
                    } catch (NumberFormatException e) {
                        failed++;
                        errors.add("Row " + rowNum + ": Invalid LANGUAGE ID");
                        continue;
                    }
                    long referenceId;
                    try {
                        referenceId = Long.parseLong(row.getReferenceId().trim());
                    } catch (NumberFormatException e) {
                        failed++;
                        errors.add("Row " + rowNum + ": Invalid REFERENCE ID");
                        continue;
                    }

                    CreateLocalizedNameRequest request = new CreateLocalizedNameRequest();
                    request.setValue(row.getValue().trim());
                    request.setLanguageId(languageId);
                    request.setReferenceType(row.getReferenceType().trim());
                    request.setReferenceId(referenceId);

                    LocalizedNameResponse response = create(request, Locale.ENGLISH, "IMPORT_SCRIPT");

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
                ? "Import completed successfully. " + success + " out of " + total + " localized names imported."
                : "Import failed. No localized names were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private LocalizedNameResponse buildLocalizedNameResponse(int statusCode, boolean isSuccess, String message,
                                                             LocalizedNameDto localizedNameDto, List<LocalizedNameDto> localizedNameDtoList,
                                                             Page<LocalizedNameDto> localizedNameDtoPage) {
        LocalizedNameResponse localizedNameResponse = new LocalizedNameResponse();
        localizedNameResponse.setStatusCode(statusCode);
        localizedNameResponse.setSuccess(isSuccess);
        localizedNameResponse.setMessage(message);
        localizedNameResponse.setLocalizedNameDto(localizedNameDto);
        localizedNameResponse.setLocalizedNameDtoList(localizedNameDtoList);
        localizedNameResponse.setLocalizedNameDtoPage(localizedNameDtoPage);
        return localizedNameResponse;
    }

    private LocalizedNameResponse buildLocalizedNameResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                                       LocalizedNameDto localizedNameDto, List<LocalizedNameDto> localizedNameDtoList,
                                                                       Page<LocalizedNameDto> localizedNameDtoPage, List<String> errorMessages) {
        LocalizedNameResponse localizedNameResponse = buildLocalizedNameResponse(statusCode, isSuccess, message, localizedNameDto, localizedNameDtoList, localizedNameDtoPage);
        localizedNameResponse.setErrorMessages(errorMessages);
        return localizedNameResponse;
    }
}
