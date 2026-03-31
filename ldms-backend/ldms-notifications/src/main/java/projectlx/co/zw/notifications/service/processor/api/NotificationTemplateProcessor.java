package projectlx.co.zw.notifications.service.processor.api;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.notifications.utils.dtos.ImportSummary;
import projectlx.co.zw.notifications.utils.requests.CreateTemplateRequest;
import projectlx.co.zw.notifications.utils.requests.TemplateMultipleFiltersRequest;
import projectlx.co.zw.notifications.utils.requests.UpdateTemplateRequest;
import projectlx.co.zw.notifications.utils.responses.TemplateResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Interface for processing notification template operations
 */
public interface NotificationTemplateProcessor {
    /**
     * Create a new notification template
     *
     * @param createTemplateRequest the request containing template details
     * @param locale the locale for internationalization
     * @param username the username of the requester
     * @return the response containing the created template
     */
    TemplateResponse create(CreateTemplateRequest createTemplateRequest, Locale locale, String username);

    /**
     * Find a notification template by ID
     *
     * @param id the ID of the template to find
     * @param locale the locale for internationalization
     * @param username the username of the requester
     * @return the response containing the found template
     */
    TemplateResponse findById(Long id, Locale locale, String username);

    /**
     * Find all notification templates as a list
     *
     * @param locale the locale for internationalization
     * @param username the username of the requester
     * @return the response containing all templates
     */
    TemplateResponse findAllAsList(Locale locale, String username);

    /**
     * Update an existing notification template
     *
     * @param updateTemplateRequest the request containing updated template details
     * @param username the username of the requester
     * @param locale the locale for internationalization
     * @return the response containing the updated template
     */
    TemplateResponse update(UpdateTemplateRequest updateTemplateRequest, String username, Locale locale);

    /**
     * Delete a notification template
     *
     * @param id the ID of the template to delete
     * @param locale the locale for internationalization
     * @param username the username of the requester
     * @return the response indicating the result of the deletion
     */
    TemplateResponse delete(Long id, Locale locale, String username);

    /**
     * Find notification templates using multiple filters
     *
     * @param templateMultipleFiltersRequest the request containing filter criteria
     * @param username the username of the requester
     * @param locale the locale for internationalization
     * @return the response containing the filtered templates
     */
    TemplateResponse findByMultipleFilters(TemplateMultipleFiltersRequest templateMultipleFiltersRequest,
                                         String username, Locale locale);

    /**
     * Export notification templates to CSV format
     *
     * @param filters the filter criteria for templates to export
     * @param username the username of the requester
     * @param locale the locale for internationalization
     * @return the CSV data as a byte array
     */
    byte[] exportToCsv(TemplateMultipleFiltersRequest filters, String username, Locale locale);

    /**
     * Export notification templates to Excel format
     *
     * @param filters the filter criteria for templates to export
     * @param username the username of the requester
     * @param locale the locale for internationalization
     * @return the Excel data as a byte array
     * @throws IOException if an I/O error occurs
     */
    byte[] exportToExcel(TemplateMultipleFiltersRequest filters, String username, Locale locale) throws IOException;

    /**
     * Export notification templates to PDF format
     *
     * @param filters the filter criteria for templates to export
     * @param username the username of the requester
     * @param locale the locale for internationalization
     * @return the PDF data as a byte array
     * @throws DocumentException if a document error occurs
     */
    byte[] exportToPdf(TemplateMultipleFiltersRequest filters, String username, Locale locale) throws DocumentException;

    /**
     * Import notification templates from a CSV file
     *
     * @param csvInputStream the input stream containing CSV data
     * @return a summary of the import operation
     * @throws IOException if an I/O error occurs
     */
    ImportSummary importTemplatesFromCsv(InputStream csvInputStream) throws IOException;

    /**
     * Get metadata for rendering the Add Template form (sections and channel options).
     * Enables a stepped, intuitive flow like Add Organization.
     *
     * @param locale the locale for internationalization
     * @param username the username of the requester
     * @return response containing addTemplateMetadata (sections + channelOptions)
     */
    TemplateResponse getAddTemplateMetadata(Locale locale, String username);
}