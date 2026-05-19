package projectlx.co.zw.notifications.business.logic.impl;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.shared_library.utils.export.LdmsExportReport;
import projectlx.co.zw.shared_library.utils.export.LdmsPdfReportWriter;
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
import projectlx.co.zw.notifications.business.auditable.api.NotificationTemplateServiceAuditable;
import projectlx.co.zw.notifications.business.logic.api.NotificationTemplateService;
import projectlx.co.zw.notifications.business.validation.api.NotificationTemplateServiceValidator;
import projectlx.co.zw.notifications.model.NotificationTemplate;
import projectlx.co.zw.notifications.repository.NotificationTemplateRepository;
import projectlx.co.zw.notifications.repository.specification.NotificationTemplateSpecification;
import projectlx.co.zw.notifications.utils.dtos.NotificationTemplateDto;
import projectlx.co.zw.notifications.utils.enums.I18Code;
import projectlx.co.zw.notifications.utils.requests.CreateTemplateRequest;
import projectlx.co.zw.notifications.utils.requests.TemplateMultipleFiltersRequest;
import projectlx.co.zw.notifications.utils.requests.UpdateTemplateRequest;
import projectlx.co.zw.notifications.utils.responses.TemplateResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
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
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.utils.dtos.ImportSummary;
import projectlx.co.zw.notifications.utils.dtos.NotificationTemplateCsvDto;

@RequiredArgsConstructor
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private static final String CSV_IMPORT_ACTOR = "IMPORT_SCRIPT";

    private final NotificationTemplateServiceValidator notificationTemplateServiceValidator;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    private final NotificationTemplateServiceAuditable notificationTemplateServiceAuditable;

    private static final String[] HEADERS = {
            "ID", "TEMPLATE KEY", "DESCRIPTION", "CHANNELS", "EMAIL SUBJECT", "SMS BODY", "IN-APP TITLE", "WHATSAPP TEMPLATE NAME", "WHATSAPP BODY",
            "ACTIVE", "CREATED AT", "UPDATED AT"
    };

    @Override
    public TemplateResponse create(CreateTemplateRequest createTemplateRequest, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = notificationTemplateServiceValidator.isCreateTemplateRequestValid(createTemplateRequest, locale);

        if (!validatorDto.getSuccess()) {
            message = messageService.getMessage(I18Code.MESSAGE_NOTIFICATION_REQUEST_IS_NULL.getCode(), new String[]{}, locale);
            return buildTemplateResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        String normalizedTemplateKey = createTemplateRequest.getTemplateKey() != null
                ? createTemplateRequest.getTemplateKey().trim()
                : null;
        createTemplateRequest.setTemplateKey(normalizedTemplateKey);
        Optional<NotificationTemplate> templateRetrieved =
                notificationTemplateRepository.findByTemplateKeyAndEntityStatusNot(normalizedTemplateKey,
                        EntityStatus.DELETED);

        if (templateRetrieved.isPresent()) {
            message = messageService.getMessage(I18Code.TEMPLATE_KEY_ALREADY_EXISTS.getCode(), new String[]{createTemplateRequest.getTemplateKey()}, locale);
            return buildTemplateResponse(400, false, message, null, null, 
                    null);
        }

        Optional<NotificationTemplate> deletedTemplate = notificationTemplateRepository.findByTemplateKey(normalizedTemplateKey)
                .filter(template -> template.getEntityStatus() == EntityStatus.DELETED);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        NotificationTemplate templateSaved;
        if (deletedTemplate.isPresent()) {
            NotificationTemplate templateToBeReactivated = deletedTemplate.get();
            templateToBeReactivated.setTemplateKey(normalizedTemplateKey);
            templateToBeReactivated.setDescription(createTemplateRequest.getDescription());
            templateToBeReactivated.setChannels(createTemplateRequest.getChannels());
            templateToBeReactivated.setEmailSubject(createTemplateRequest.getEmailSubject());
            templateToBeReactivated.setEmailBodyHtml(createTemplateRequest.getEmailBodyHtml());
            templateToBeReactivated.setSmsBody(createTemplateRequest.getSmsBody());
            templateToBeReactivated.setInAppTitle(createTemplateRequest.getInAppTitle());
            templateToBeReactivated.setInAppBody(createTemplateRequest.getInAppBody());
            templateToBeReactivated.setWhatsappTemplateName(createTemplateRequest.getWhatsappTemplateName());
            templateToBeReactivated.setWhatsappBody(createTemplateRequest.getWhatsappBody());
            templateToBeReactivated.setActive(true);
            templateToBeReactivated.setEntityStatus(EntityStatus.ACTIVE);
            templateSaved = notificationTemplateServiceAuditable.update(templateToBeReactivated, locale, username);
        } else {
            NotificationTemplate templateToBeSaved = modelMapper.map(createTemplateRequest, NotificationTemplate.class);
            // New templates should default to active unless explicitly deactivated later by admin action.
            templateToBeSaved.setActive(true);
            templateToBeSaved.setEntityStatus(EntityStatus.ACTIVE);
            templateSaved = notificationTemplateServiceAuditable.create(templateToBeSaved, locale, username);
        }

        NotificationTemplateDto templateDtoReturned = modelMapper.map(templateSaved, NotificationTemplateDto.class);

        message = messageService.getMessage(I18Code.TEMPLATE_CREATED_SUCCESSFULLY.getCode(), new String[]{}, locale);

        return buildTemplateResponse(201, true, message, templateDtoReturned, null, 
                null);
    }

    @Override
    public TemplateResponse findById(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = notificationTemplateServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.TEMPLATE_INVALID_ID.getCode(), new String[]{}, locale);

            return buildTemplateResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<NotificationTemplate> templateRetrieved = notificationTemplateRepository.findById(id);

        if (templateRetrieved.isEmpty() || templateRetrieved.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.TEMPLATE_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildTemplateResponse(404, false, message, null, null,
                    null);
        }

        NotificationTemplate templateReturned = templateRetrieved.get();
        NotificationTemplateDto templateDto = modelMapper.map(templateReturned, NotificationTemplateDto.class);

        message = messageService.getMessage(I18Code.TEMPLATE_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildTemplateResponse(200, true, message, templateDto, null,
                null);
    }

    @Override
    public TemplateResponse findAllAsList(Locale locale, String username) {

        String message = "";

        List<NotificationTemplate> templateList = notificationTemplateRepository.findAll().stream()
                .filter(template -> template.getEntityStatus() != EntityStatus.DELETED)
                .collect(Collectors.toList());

        if (templateList.isEmpty()) {

            message = messageService.getMessage(I18Code.NO_TEMPLATES_FOUND.getCode(), new String[]{}, locale);

            return buildTemplateResponse(404, false, message, null, null, null);
        }

        List<NotificationTemplateDto> templateDtoList = modelMapper.map(templateList, new TypeToken<List<NotificationTemplateDto>>(){}.getType());

        message = messageService.getMessage(I18Code.TEMPLATES_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildTemplateResponse(200, true, message, null, templateDtoList, null);
    }

    @Override
    public TemplateResponse update(UpdateTemplateRequest updateTemplateRequest, String username, Locale locale) {

        String message = "";

        ValidatorDto validatorDto = notificationTemplateServiceValidator.isUpdateTemplateRequestValid(updateTemplateRequest, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.TEMPLATE_INVALID_UPDATE_REQUEST.getCode(), new String[]{}, locale);

            return buildTemplateResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<NotificationTemplate> templateRetrieved = notificationTemplateRepository.findById(updateTemplateRequest.getId());

        if (templateRetrieved.isEmpty() || templateRetrieved.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.TEMPLATE_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildTemplateResponse(404, false, message, null, null,
                    null);
        }

        NotificationTemplate templateToBeUpdated = templateRetrieved.get();

        if (updateTemplateRequest.isStatusOnlyUpdate()) {
            templateToBeUpdated.setActive(updateTemplateRequest.getActive());
            NotificationTemplate templateUpdated =
                    notificationTemplateServiceAuditable.update(templateToBeUpdated, locale, username);
            NotificationTemplateDto templateDtoReturned = modelMapper.map(templateUpdated, NotificationTemplateDto.class);
            message = messageService.getMessage(I18Code.TEMPLATE_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
            return buildTemplateResponse(200, true, message, templateDtoReturned, null, null);
        }

        String normalizedTemplateKey = updateTemplateRequest.getTemplateKey() != null
                ? updateTemplateRequest.getTemplateKey().trim()
                : null;
        Optional<NotificationTemplate> templateByKey =
                notificationTemplateRepository.findByTemplateKeyAndEntityStatusNot(normalizedTemplateKey, EntityStatus.DELETED);
        if (templateByKey.isPresent() && !templateByKey.get().getId().equals(templateToBeUpdated.getId())) {
            message = messageService.getMessage(I18Code.TEMPLATE_KEY_ALREADY_EXISTS.getCode(),
                    new String[]{normalizedTemplateKey}, locale);
            return buildTemplateResponse(400, false, message, null, null, null);
        }

        templateToBeUpdated.setTemplateKey(normalizedTemplateKey);
        templateToBeUpdated.setDescription(updateTemplateRequest.getDescription());
        templateToBeUpdated.setChannels(updateTemplateRequest.getChannels());
        templateToBeUpdated.setEmailSubject(updateTemplateRequest.getEmailSubject());
        templateToBeUpdated.setEmailBodyHtml(updateTemplateRequest.getEmailBodyHtml());
        templateToBeUpdated.setSmsBody(updateTemplateRequest.getSmsBody());
        templateToBeUpdated.setInAppTitle(updateTemplateRequest.getInAppTitle());
        templateToBeUpdated.setInAppBody(updateTemplateRequest.getInAppBody());
        templateToBeUpdated.setWhatsappTemplateName(updateTemplateRequest.getWhatsappTemplateName());
        templateToBeUpdated.setWhatsappBody(updateTemplateRequest.getWhatsappBody());
        if (updateTemplateRequest.getActive() != null) {
            templateToBeUpdated.setActive(updateTemplateRequest.getActive());
        }

        NotificationTemplate templateUpdated =
                notificationTemplateServiceAuditable.update(templateToBeUpdated, locale, username);

        NotificationTemplateDto templateDtoReturned = modelMapper.map(templateUpdated, NotificationTemplateDto.class);

        message = messageService.getMessage(I18Code.TEMPLATE_UPDATED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildTemplateResponse(200, true, message, templateDtoReturned, null,
                null);
    }

    @Override
    public TemplateResponse delete(Long id, Locale locale, String username) {

        String message = "";

        ValidatorDto validatorDto = notificationTemplateServiceValidator.isIdValid(id, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.TEMPLATE_INVALID_ID.getCode(), new String[]{}, locale);

            return buildTemplateResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Optional<NotificationTemplate> templateRetrieved = notificationTemplateRepository.findById(id);

        if (templateRetrieved.isEmpty() || templateRetrieved.get().getEntityStatus() == EntityStatus.DELETED) {

            message = messageService.getMessage(I18Code.TEMPLATE_NOT_FOUND.getCode(), new String[]{}, locale);

            return buildTemplateResponse(404, false, message, null, null,
                    null);
        }

        NotificationTemplate templateToBeDeleted = templateRetrieved.get();
        templateToBeDeleted.setEntityStatus(EntityStatus.DELETED);

        NotificationTemplate templateDeleted =
                notificationTemplateServiceAuditable.delete(templateToBeDeleted, locale, username);

        NotificationTemplateDto templateDtoReturned = modelMapper.map(templateDeleted, NotificationTemplateDto.class);

        message = messageService.getMessage(I18Code.TEMPLATE_DELETED_SUCCESSFULLY.getCode(), new String[]{}, locale);
        return buildTemplateResponse(200, true, message, templateDtoReturned, null, null);
    }

    @Override
    public TemplateResponse findByMultipleFilters(TemplateMultipleFiltersRequest request, String username, Locale locale) {

        String message = "";

        Specification<NotificationTemplate> spec = null;
        spec = addToSpec(spec, NotificationTemplateSpecification::deleted);

        ValidatorDto validatorDto = notificationTemplateServiceValidator.isTemplateMultipleFiltersRequestValid(request, locale);

        if (!validatorDto.getSuccess()) {

            message = messageService.getMessage(I18Code.TEMPLATE_INVALID_FILTER_REQUEST.getCode(), new String[]{}, locale);

            return buildTemplateResponseWithErrors(400, false, message, null, null,
                    null, validatorDto.getErrorMessages());
        }

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

        if (isNotEmpty(request.getTemplateKey())) {
            spec = addToSpec(request.getTemplateKey(), spec, NotificationTemplateSpecification::templateKeyLike);
        }

        if (isNotEmpty(request.getInAppTitle())) {
            spec = addToSpec(request.getInAppTitle(), spec, NotificationTemplateSpecification::inAppTitleLike);
        }

        if (isNotEmpty(request.getWhatsappTemplateName())) {
            spec = addToSpec(request.getWhatsappTemplateName(), spec, NotificationTemplateSpecification::whatsappTemplateNameLike);
        }

        if (request.getChannels() != null && !request.getChannels().isEmpty()) {
            for (String channel : request.getChannels()) {
                spec = addToSpec(channel, spec, NotificationTemplateSpecification::hasChannel);
            }
        }

        if (isNotEmpty(request.getSearchValue())) {
            spec = addToSpec(request.getSearchValue(), spec, NotificationTemplateSpecification::any);
        }

        if (request.getIsActive() != null) {
            spec = addToSpec(request.getIsActive(), spec, NotificationTemplateSpecification::isActive);
        }

        Page<NotificationTemplate> result = notificationTemplateRepository.findAll(spec, pageable);

        if (result.getContent().isEmpty()) {
            message = messageService.getMessage(I18Code.NO_TEMPLATES_FOUND.getCode(), new String[]{}, locale);
            return buildTemplateResponse(404, false, message, null, null,
                    null);
        }

        Page<NotificationTemplateDto> templateDtoPage = convertTemplateEntityToTemplateDto(result);

        message = messageService.getMessage(I18Code.TEMPLATES_RETRIEVED_SUCCESSFULLY.getCode(), new String[]{}, locale);

        return buildTemplateResponse(200, true, message, null, null, templateDtoPage);
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    @Override
    public byte[] exportToCsv(List<NotificationTemplateDto> templates) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");

        for (NotificationTemplateDto template : templates) {
            sb.append(template.getId()).append(",")
                    .append(safe(template.getTemplateKey())).append(",")
                    .append(safe(template.getDescription())).append(",")
                    .append(template.getChannels() != null ? template.getChannels().toString() : "").append(",")
                    .append(safe(template.getEmailSubject())).append(",")
                    .append(safe(template.getSmsBody())).append(",")
                    .append(safe(template.getInAppTitle())).append(",")
                    .append(safe(template.getWhatsappTemplateName())).append(",")
                    .append(safe(template.getWhatsappBody())).append(",")
                    .append(template.isActive()).append(",")
                    .append(template.getCreatedAt()).append(",")
                    .append(template.getUpdatedAt()).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportToExcel(List<NotificationTemplateDto> templates) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Templates");

            Row header = sheet.createRow(0);

            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            int rowIdx = 1;

            for (NotificationTemplateDto template : templates) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(template.getId());
                row.createCell(1).setCellValue(safe(template.getTemplateKey()));
                row.createCell(2).setCellValue(safe(template.getDescription()));
                row.createCell(3).setCellValue(template.getChannels() != null ? template.getChannels().toString() : "");
                row.createCell(4).setCellValue(safe(template.getEmailSubject()));
                row.createCell(5).setCellValue(safe(template.getSmsBody()));
                row.createCell(6).setCellValue(safe(template.getInAppTitle()));
                row.createCell(7).setCellValue(safe(template.getWhatsappTemplateName()));
                row.createCell(8).setCellValue(safe(template.getWhatsappBody()));
                row.createCell(9).setCellValue(template.isActive());
                row.createCell(10).setCellValue(template.getCreatedAt() != null ? template.getCreatedAt().toString() : "");
                row.createCell(11).setCellValue(template.getUpdatedAt() != null ? template.getUpdatedAt().toString() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(List<NotificationTemplateDto> templates) throws DocumentException {
        List<String[]> rows = new ArrayList<>();
        for (NotificationTemplateDto template : templates) {
            rows.add(new String[]{
                    String.valueOf(template.getId()),
                    safe(template.getTemplateKey()),
                    safe(template.getDescription()),
                    template.getChannels() != null ? template.getChannels().toString() : "",
                    safe(template.getEmailSubject()),
                    safe(template.getSmsBody()),
                    safe(template.getInAppTitle()),
                    safe(template.getWhatsappTemplateName()),
                    safe(template.getWhatsappBody()),
                    String.valueOf(template.isActive()),
                    template.getCreatedAt() != null ? template.getCreatedAt().toString() : "",
                    template.getUpdatedAt() != null ? template.getUpdatedAt().toString() : ""
            });
        }
        return LdmsPdfReportWriter.write(LdmsExportReport.builder()
                .title("Notification Templates")
                .reportCode("NTF-TPL")
                .subtitle("Notification template registry export")
                .columnHeaders(HEADERS)
                .rows(rows)
                .landscape(true)
                .build());
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private Specification<NotificationTemplate> addToSpec(Specification<NotificationTemplate> spec,
                                                      Function<EntityStatus, Specification<NotificationTemplate>> predicateMethod) {
        Specification<NotificationTemplate> localSpec = Specification.where(predicateMethod.apply(EntityStatus.DELETED));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Specification<NotificationTemplate> addToSpec(final String aString, Specification<NotificationTemplate> spec, 
                                                      Function<String, Specification<NotificationTemplate>> predicateMethod) {
        if (aString != null && !aString.isEmpty()) {
            Specification<NotificationTemplate> localSpec = Specification.where(predicateMethod.apply(aString));
            spec = (spec == null) ? localSpec : spec.and(localSpec);
            return spec;
        }
        return spec;
    }

    private Specification<NotificationTemplate> addToSpec(final Boolean isActive, Specification<NotificationTemplate> spec,
                                                      Function<Boolean, Specification<NotificationTemplate>> predicateMethod) {
        if (isActive == null) {
            return spec;
        }
        Specification<NotificationTemplate> localSpec = Specification.where(predicateMethod.apply(isActive));
        spec = (spec == null) ? localSpec : spec.and(localSpec);
        return spec;
    }

    private Page<NotificationTemplateDto> convertTemplateEntityToTemplateDto(Page<NotificationTemplate> templatePage) {
        List<NotificationTemplate> templateList = templatePage.getContent();
        List<NotificationTemplateDto> templateDtoList = new ArrayList<>();

        for (NotificationTemplate template : templatePage) {
            NotificationTemplateDto templateDto = modelMapper.map(template, NotificationTemplateDto.class);
            templateDtoList.add(templateDto);
        }

        int page = templatePage.getNumber();
        int size = templatePage.getSize();

        size = size <= 0 ? 10 : size;

        Pageable pageableTemplates = PageRequest.of(page, size);

        return new PageImpl<>(templateDtoList, pageableTemplates, templatePage.getTotalElements());
    }

    private TemplateResponse buildTemplateResponse(int statusCode, boolean isSuccess, String message,
                                                NotificationTemplateDto templateDto, List<NotificationTemplateDto> templateDtoList,
                                                Page<NotificationTemplateDto> templateDtoPage) {
        TemplateResponse templateResponse = new TemplateResponse();

        templateResponse.setStatusCode(statusCode);
        templateResponse.setSuccess(isSuccess);
        templateResponse.setMessage(message);
        templateResponse.setTemplate(templateDto);
        templateResponse.setTemplateList(templateDtoList);
        templateResponse.setTemplatePage(templateDtoPage);

        return templateResponse;
    }

    private TemplateResponse buildTemplateResponseWithErrors(int statusCode, boolean isSuccess, String message,
                                                          NotificationTemplateDto templateDto, List<NotificationTemplateDto> templateDtoList,
                                                          Page<NotificationTemplateDto> templateDtoPage, List<String> errorMessages) {
        TemplateResponse templateResponse = new TemplateResponse();

        templateResponse.setStatusCode(statusCode);
        templateResponse.setSuccess(isSuccess);
        templateResponse.setMessage(message);
        templateResponse.setTemplate(templateDto);
        templateResponse.setTemplateList(templateDtoList);
        templateResponse.setTemplatePage(templateDtoPage);
        templateResponse.setErrorMessages(errorMessages);

        return templateResponse;
    }

    @Override
    public ImportSummary importTemplatesFromCsv(InputStream csvInputStream) throws IOException {

        List<String> errors = new ArrayList<>();
        int success = 0, failed = 0, total = 0;

        try (Reader reader = new InputStreamReader(csvInputStream, StandardCharsets.UTF_8)) {
            // Configure CSV to Bean mapping
            HeaderColumnNameMappingStrategy<NotificationTemplateCsvDto> strategy = new HeaderColumnNameMappingStrategy<>();
            strategy.setType(NotificationTemplateCsvDto.class);

            CsvToBean<NotificationTemplateCsvDto> csvToBean = new CsvToBeanBuilder<NotificationTemplateCsvDto>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            // Parse CSV to list of DTOs
            List<NotificationTemplateCsvDto> templatesList = csvToBean.parse();
            total = templatesList.size();

            int rowNum = 1; // Start from 1 to account for header
            for (NotificationTemplateCsvDto templateDto : templatesList) {
                rowNum++;
                try {
                    if (templateDto.getTemplateKey() == null || templateDto.getTemplateKey().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing template key");
                        continue;
                    }

                    if (templateDto.getDescription() == null || templateDto.getDescription().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing description");
                        continue;
                    }

                    if (templateDto.getChannels() == null || templateDto.getChannels().isEmpty()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Missing channels");
                        continue;
                    }

                    // Parse channels from CSV string (comma-separated list of channel names)
                    List<Channel> channelsList = new ArrayList<>();
                    String[] channelNames = templateDto.getChannels().split(",");
                    boolean invalidChannel = false;
                    for (String channelName : channelNames) {
                        try {
                            channelsList.add(Channel.valueOf(channelName.trim()));
                        } catch (IllegalArgumentException e) {
                            failed++;
                            errors.add("Row " + rowNum + ": Invalid channel name: " + channelName.trim());
                            invalidChannel = true;
                            break;
                        }
                    }
                    if (invalidChannel) {
                        continue;
                    }

                    // Create a new template
                    NotificationTemplate template = new NotificationTemplate();
                    template.setTemplateKey(templateDto.getTemplateKey());
                    template.setDescription(templateDto.getDescription());
                    template.setChannels(channelsList);
                    template.setEmailSubject(templateDto.getEmailSubject());
                    template.setEmailBodyHtml(templateDto.getEmailBodyHtml());
                    template.setSmsBody(templateDto.getSmsBody());
                    template.setInAppTitle(templateDto.getInAppTitle());
                    template.setInAppBody(templateDto.getInAppBody());
                    template.setWhatsappTemplateName(templateDto.getWhatsappTemplateName());
                    template.setWhatsappBody(templateDto.getWhatsappBody());

                    // Parse active status
                    if (templateDto.getActive() != null && !templateDto.getActive().isEmpty()) {
                        template.setActive(Boolean.parseBoolean(templateDto.getActive()));
                    } else {
                        template.setActive(true); // Default to active
                    }

                    String normalizedTemplateKey = template.getTemplateKey() != null
                            ? template.getTemplateKey().trim()
                            : null;
                    template.setTemplateKey(normalizedTemplateKey);

                    // Check if template with same key already exists
                    Optional<NotificationTemplate> existingTemplate =
                            notificationTemplateRepository.findByTemplateKeyAndEntityStatusNot(
                                    normalizedTemplateKey, EntityStatus.DELETED);

                    if (existingTemplate.isPresent()) {
                        failed++;
                        errors.add("Row " + rowNum + ": Template with key '" + template.getTemplateKey() + "' already exists");
                        continue;
                    }
                    Optional<NotificationTemplate> deletedTemplate = notificationTemplateRepository.findByTemplateKey(normalizedTemplateKey)
                            .filter(existing -> existing.getEntityStatus() == EntityStatus.DELETED);
                    if (deletedTemplate.isPresent()) {
                        NotificationTemplate templateToBeReactivated = deletedTemplate.get();
                        templateToBeReactivated.setDescription(template.getDescription());
                        templateToBeReactivated.setChannels(template.getChannels());
                        templateToBeReactivated.setEmailSubject(template.getEmailSubject());
                        templateToBeReactivated.setEmailBodyHtml(template.getEmailBodyHtml());
                        templateToBeReactivated.setSmsBody(template.getSmsBody());
                        templateToBeReactivated.setInAppTitle(template.getInAppTitle());
                        templateToBeReactivated.setInAppBody(template.getInAppBody());
                        templateToBeReactivated.setWhatsappTemplateName(template.getWhatsappTemplateName());
                        templateToBeReactivated.setWhatsappBody(template.getWhatsappBody());
                        templateToBeReactivated.setActive(template.isActive());
                        templateToBeReactivated.setEntityStatus(EntityStatus.ACTIVE);
                        notificationTemplateServiceAuditable.update(templateToBeReactivated, Locale.ENGLISH, CSV_IMPORT_ACTOR);
                    } else {
                        notificationTemplateServiceAuditable.create(template, Locale.ENGLISH, CSV_IMPORT_ACTOR);
                    }
                    success++;

                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + rowNum + ": Unexpected error - " + e.getMessage());
                }
            }
        }

        // Determine status code and success flag based on import results
        int statusCode = success > 0 ? 200 : 400;
        boolean isSuccess = success > 0;
        String message = success > 0
                ? "Import completed successfully. " + success + " out of " + total + " templates imported."
                : "Import failed. No templates were imported.";

        return new ImportSummary(statusCode, isSuccess, message, total, success, failed, errors);
    }
}
