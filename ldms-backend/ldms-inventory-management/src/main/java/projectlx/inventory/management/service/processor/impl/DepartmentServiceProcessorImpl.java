package projectlx.inventory.management.service.processor.impl;

import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.inventory.management.business.logic.api.DepartmentService;
import projectlx.inventory.management.service.processor.api.DepartmentServiceProcessor;
import projectlx.inventory.management.utils.dtos.DepartmentDto;
import projectlx.inventory.management.utils.dtos.ImportSummary;
import projectlx.inventory.management.utils.requests.CreateDepartmentRequest;
import projectlx.inventory.management.utils.requests.DepartmentMultipleFiltersRequest;
import projectlx.inventory.management.utils.requests.EditDepartmentRequest;
import projectlx.inventory.management.utils.responses.DepartmentResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class DepartmentServiceProcessorImpl implements DepartmentServiceProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentServiceProcessorImpl.class);
    private final DepartmentService departmentService;

    @Override
    public DepartmentResponse create(CreateDepartmentRequest request, Locale locale, String username) {
        logger.info("Incoming request to create department for user: {}", username);
        DepartmentResponse response = departmentService.create(request, locale, username);
        logger.info("Outgoing response after creating department: success={}", response != null && response.isSuccess());
        return response;
    }

    @Override
    public DepartmentResponse findById(Long id, Locale locale, String username) {
        logger.info("Incoming request to find department by id {} for user: {}", id, username);
        return departmentService.findById(id, locale, username);
    }

    @Override
    public DepartmentResponse findAllAsList(Locale locale, String username) {
        logger.info("Incoming request to list departments for user: {}", username);
        return departmentService.findAllAsList(locale, username);
    }

    @Override
    public DepartmentResponse findByMultipleFilters(
            DepartmentMultipleFiltersRequest request, String username, Locale locale) {
        logger.info("Incoming request to find departments by multiple filters for user: {}", username);
        return departmentService.findByMultipleFilters(request, username, locale);
    }

    @Override
    public DepartmentResponse update(EditDepartmentRequest request, String username, Locale locale) {
        logger.info("Incoming request to update department for user: {}", username);
        return departmentService.update(request, username, locale);
    }

    @Override
    public DepartmentResponse delete(Long id, Locale locale, String username) {
        logger.info("Incoming request to delete department {} for user: {}", id, username);
        return departmentService.delete(id, locale, username);
    }

    @Override
    public byte[] exportToCsv(List<DepartmentDto> items) {
        return departmentService.exportToCsv(items);
    }

    @Override
    public byte[] exportToExcel(List<DepartmentDto> items) throws IOException {
        return departmentService.exportToExcel(items);
    }

    @Override
    public byte[] exportToPdf(List<DepartmentDto> items) throws DocumentException {
        return departmentService.exportToPdf(items);
    }

    @Override
    public ImportSummary importDepartmentFromCsv(InputStream csvInputStream, String username, Locale locale)
            throws IOException {
        logger.info("Incoming request to import departments from CSV for user: {}", username);
        return departmentService.importDepartmentFromCsv(csvInputStream, username, locale);
    }
}
