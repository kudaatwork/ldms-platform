package projectlx.co.zw.notifications.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import projectlx.co.zw.notifications.business.logic.api.NotificationTemplateService;
import projectlx.co.zw.notifications.service.processor.api.NotificationTemplateProcessor;
import projectlx.co.zw.notifications.model.Channel;
import projectlx.co.zw.notifications.utils.dtos.ChannelOptionDto;
import projectlx.co.zw.notifications.utils.dtos.ImportSummary;
import projectlx.co.zw.notifications.utils.dtos.NotificationTemplateDto;
import projectlx.co.zw.notifications.utils.dtos.TemplateCreationMetadataDto;
import projectlx.co.zw.notifications.utils.dtos.TemplateFormSectionDto;
import projectlx.co.zw.notifications.utils.requests.CreateTemplateRequest;
import projectlx.co.zw.notifications.utils.requests.TemplateMultipleFiltersRequest;
import projectlx.co.zw.notifications.utils.requests.UpdateTemplateRequest;
import projectlx.co.zw.notifications.utils.responses.TemplateResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class NotificationTemplateProcessorImpl implements NotificationTemplateProcessor {

    private final NotificationTemplateService notificationTemplateService;
    private final Logger logger = LoggerFactory.getLogger(NotificationTemplateProcessorImpl.class);

    @Override
    public TemplateResponse create(CreateTemplateRequest createTemplateRequest, Locale locale, String username) {
        logger.info("Incoming request to create a notification template: {}", createTemplateRequest);

        TemplateResponse templateResponse = notificationTemplateService.create(createTemplateRequest, locale, username);

        logger.info("Outgoing response after creating a notification template: {}. Status Code: {}. Message: {}", 
                templateResponse, templateResponse.getStatusCode(), templateResponse.getMessage());

        return templateResponse;
    }

    @Override
    public TemplateResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find a notification template by id: {}", id);

        TemplateResponse templateResponse = notificationTemplateService.findById(id, locale, username);

        logger.info("Outgoing response after finding a notification template by id: {}. Status Code: {}. Message: {}", 
                templateResponse, templateResponse.getStatusCode(), templateResponse.getMessage());

        return templateResponse;
    }

    @Override
    public TemplateResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to find all notification templates as a list");

        TemplateResponse templateResponse = notificationTemplateService.findAllAsList(locale, username);

        logger.info("Outgoing response after finding all notification templates as a list: {}. Status Code: {}. Message: {}", 
                templateResponse, templateResponse.getStatusCode(), templateResponse.getMessage());

        return templateResponse;
    }

    @Override
    public TemplateResponse update(UpdateTemplateRequest updateTemplateRequest, String username, Locale locale) {
        logger.info("Incoming request to update a notification template: {}", updateTemplateRequest);

        TemplateResponse templateResponse = notificationTemplateService.update(updateTemplateRequest, username, locale);

        logger.info("Outgoing response after updating a notification template: {}. Status Code: {}. Message: {}", 
                templateResponse, templateResponse.getStatusCode(), templateResponse.getMessage());

        return templateResponse;
    }

    @Override
    public TemplateResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete a notification template with the id: {}", id);

        TemplateResponse templateResponse = notificationTemplateService.delete(id, locale, username);

        logger.info("Outgoing response after deleting a notification template: {}. Status Code: {}. Message: {}", 
                templateResponse, templateResponse.getStatusCode(), templateResponse.getMessage());

        return templateResponse;
    }

    @Override
    public TemplateResponse findByMultipleFilters(TemplateMultipleFiltersRequest templateMultipleFiltersRequest, String username, Locale locale) {
        logger.info("Incoming request to find notification templates using multiple filters: {}", templateMultipleFiltersRequest);

        TemplateResponse templateResponse = notificationTemplateService.findByMultipleFilters(templateMultipleFiltersRequest, username, locale);

        logger.info("Outgoing response after finding notification templates using multiple filters: {}. Status Code: {}. Message: {}", 
                templateResponse, templateResponse.getStatusCode(), templateResponse.getMessage());

        return templateResponse;
    }

    @Override
    public byte[] exportToCsv(TemplateMultipleFiltersRequest filters, String username, Locale locale) {
        logger.info("Incoming request to export notification templates to CSV using filters: {}", filters);

        TemplateResponse templateResponse = notificationTemplateService.findByMultipleFilters(filters, username, locale);

        List<NotificationTemplateDto> templateList = Optional.ofNullable(templateResponse.getTemplatePage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] csvData = notificationTemplateService.exportToCsv(templateList);

        logger.info("Outgoing CSV export complete. Byte size: {}", csvData.length);

        return csvData;
    }

    @Override
    public byte[] exportToExcel(TemplateMultipleFiltersRequest filters, String username, Locale locale) throws IOException {
        logger.info("Incoming request to export notification templates to Excel using filters: {}", filters);

        TemplateResponse templateResponse = notificationTemplateService.findByMultipleFilters(filters, username, locale);

        List<NotificationTemplateDto> templateList = Optional.ofNullable(templateResponse.getTemplatePage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] excelData = notificationTemplateService.exportToExcel(templateList);

        logger.info("Outgoing Excel export complete. Byte size: {}", excelData.length);

        return excelData;
    }

    @Override
    public byte[] exportToPdf(TemplateMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException {
        logger.info("Incoming request to export notification templates to PDF using filters: {}", filters);

        TemplateResponse templateResponse = notificationTemplateService.findByMultipleFilters(filters, username, locale);

        List<NotificationTemplateDto> templateList = Optional.ofNullable(templateResponse.getTemplatePage())
                .map(Page::getContent)
                .orElse(Collections.emptyList());

        byte[] pdfData = notificationTemplateService.exportToPdf(templateList);

        logger.info("Outgoing PDF export complete. Byte size: {}", pdfData.length);

        return pdfData;
    }

    @Override
    public ImportSummary importTemplatesFromCsv(InputStream csvInputStream) throws IOException {
        logger.info("Incoming request to import notification templates from CSV");

        ImportSummary summary = notificationTemplateService.importTemplatesFromCsv(csvInputStream);

        logger.info("Outgoing response after importing notification templates from CSV: {}. Status Code: {}. Message: {}", 
                summary, summary.getStatusCode(), summary.getMessage());

        return summary;
    }

    @Override
    public TemplateResponse getAddTemplateMetadata(Locale locale, String username) {
        logger.info("Incoming request for Add Template form metadata");

        List<TemplateFormSectionDto> sections = List.of(
                TemplateFormSectionDto.builder()
                        .sectionKey("identity")
                        .sectionLabel("Template identity")
                        .sectionDescription("Basic identifier and description (like Organization name and type).")
                        .order(1)
                        .fieldKeys(List.of("templateKey", "description"))
                        .build(),
                TemplateFormSectionDto.builder()
                        .sectionKey("channels")
                        .sectionLabel("Delivery channels")
                        .sectionDescription("Select where this template will be sent (Email, SMS, In-app, WhatsApp).")
                        .order(2)
                        .fieldKeys(List.of("channels"))
                        .build(),
                TemplateFormSectionDto.builder()
                        .sectionKey("email")
                        .sectionLabel("Email content")
                        .sectionDescription("Subject and HTML body. Shown when Email is selected. Use {{placeholder}} for variables.")
                        .order(3)
                        .fieldKeys(List.of("emailSubject", "emailBodyHtml"))
                        .build(),
                TemplateFormSectionDto.builder()
                        .sectionKey("sms")
                        .sectionLabel("SMS content")
                        .sectionDescription("Message body for SMS (max 320 characters). Shown when SMS is selected.")
                        .order(4)
                        .fieldKeys(List.of("smsBody"))
                        .build(),
                TemplateFormSectionDto.builder()
                        .sectionKey("inApp")
                        .sectionLabel("In-app content")
                        .sectionDescription("Title and body for in-app notifications. Shown when In-app is selected.")
                        .order(5)
                        .fieldKeys(List.of("inAppTitle", "inAppBody"))
                        .build(),
                TemplateFormSectionDto.builder()
                        .sectionKey("whatsapp")
                        .sectionLabel("WhatsApp")
                        .sectionDescription("Twilio/WhatsApp template name (Content SID). Shown when WhatsApp is selected.")
                        .order(6)
                        .fieldKeys(List.of("whatsappTemplateName"))
                        .build()
        );

        List<ChannelOptionDto> channelOptions = Stream.of(Channel.values())
                .map(c -> ChannelOptionDto.builder()
                        .value(c.name())
                        .label(c.name().replace("_", " "))
                        .description(channelDescription(c))
                        .build())
                .toList();

        TemplateCreationMetadataDto metadata = TemplateCreationMetadataDto.builder()
                .sections(sections)
                .channelOptions(channelOptions)
                .build();

        TemplateResponse response = new TemplateResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setAddTemplateMetadata(metadata);

        logger.info("Returning Add Template metadata with {} sections and {} channel options", sections.size(), channelOptions.size());
        return response;
    }

    private static String channelDescription(Channel c) {
        return switch (c) {
            case EMAIL -> "Send as email (subject + HTML body).";
            case SMS -> "Send as SMS (max 320 chars).";
            case IN_APP -> "Show in-app notification (title + body).";
            case WHATSAPP -> "Send via WhatsApp (Twilio template name required).";
            case SLACK -> "Send to Slack via incoming webhook.";
            case TEAMS -> "Send to Microsoft Teams via incoming webhook.";
        };
    }
}