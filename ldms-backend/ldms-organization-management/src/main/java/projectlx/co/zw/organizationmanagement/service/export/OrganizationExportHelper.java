package projectlx.co.zw.organizationmanagement.service.export;

import com.lowagie.text.DocumentException;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryDto;
import projectlx.co.zw.shared_library.utils.dtos.AgentDto;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.dtos.OrganizationDto;
import projectlx.co.zw.shared_library.utils.export.LdmsExcelReportWriter;
import projectlx.co.zw.shared_library.utils.export.LdmsExportMediaTypes;
import projectlx.co.zw.shared_library.utils.export.LdmsExportReport;
import projectlx.co.zw.shared_library.utils.export.LdmsPdfReportWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts organization-domain export DTOs (Branch, Agent, Industry) to
 * CSV / XLSX / PDF byte arrays using the shared LDMS export writers.
 */
@Component
public class OrganizationExportHelper {

    private static final String[] BRANCH_HEADERS = {
            "ID", "ORGANIZATION ID", "ORGANIZATION NAME", "BRANCH NAME", "BRANCH CODE",
            "REGION", "EMAIL", "PHONE NUMBER", "HEAD OFFICE", "ACTIVE", "BUSINESS HOURS", "LOCATION ID"
    };

    private static final String[] AGENT_HEADERS = {
            "ID", "ORGANIZATION ID", "ORGANIZATION NAME", "AGENT KIND", "FIRST NAME", "LAST NAME",
            "EMAIL", "PHONE NUMBER", "AGENT TYPE", "ROLE", "BRANCH ID", "ACTIVE",
            "LOCATION ID", "ASSIGNED REGION"
    };

    private static final String[] INDUSTRY_HEADERS = {
            "ID", "NAME", "INDUSTRY CODE", "DESCRIPTION",
            "REGULATORY BODY NAME", "REGULATORY BODY CONTACT", "COMPLIANCE REQUIREMENTS", "ACTIVE"
    };

    private static final String[] ORGANIZATION_HEADERS = {
            "ID", "NAME", "EMAIL", "PHONE", "CLASSIFICATION", "TYPE", "INDUSTRY", "REGISTRATION",
            "TAX NUMBER", "KYC STATUS", "VERIFIED", "SOURCE", "CREATED AT"
    };

    // =========================================================
    // Organization
    // =========================================================

    public byte[] organizationsToBytes(List<OrganizationDto> items, String format) throws IOException, DocumentException {
        List<String[]> rows = new ArrayList<>();
        for (OrganizationDto o : items) {
            rows.add(new String[]{
                    str(o.getId()),
                    o.getName(),
                    o.getEmail(),
                    o.getPhoneNumber(),
                    o.getOrganizationClassification() != null ? o.getOrganizationClassification().name() : "",
                    o.getOrganizationType() != null ? o.getOrganizationType().name() : "",
                    o.getIndustryName(),
                    o.getRegistrationNumber(),
                    o.getTaxNumber(),
                    o.getKycStatus(),
                    String.valueOf(Boolean.TRUE.equals(o.getIsVerified())),
                    Boolean.TRUE.equals(o.getCreatedViaSignup()) ? "Platform signup" : "Admin registration",
                    o.getCreatedAt() != null ? o.getCreatedAt().toString() : ""
            });
        }
        return toBytes("Organizations", "ORGANIZATIONS", ORGANIZATION_HEADERS, rows, format);
    }

    // =========================================================
    // Branch
    // =========================================================

    public byte[] branchesToBytes(List<BranchDto> items, String format) throws IOException, DocumentException {
        List<String[]> rows = new ArrayList<>();
        for (BranchDto b : items) {
            rows.add(new String[]{
                    str(b.getId()),
                    str(b.getOrganizationId()),
                    b.getOrganizationName(),
                    b.getBranchName(),
                    b.getBranchCode(),
                    b.getRegion(),
                    b.getEmail(),
                    b.getPhoneNumber(),
                    String.valueOf(b.isHeadOffice()),
                    String.valueOf(b.isActive()),
                    b.getBusinessHours(),
                    str(b.getLocationId())
            });
        }
        return toBytes("Branches", "BRANCHES", BRANCH_HEADERS, rows, format);
    }

    // =========================================================
    // Agent
    // =========================================================

    public byte[] agentsToBytes(List<AgentDto> items, String format) throws IOException, DocumentException {
        List<String[]> rows = new ArrayList<>();
        for (AgentDto a : items) {
            rows.add(new String[]{
                    str(a.getId()),
                    str(a.getOrganizationId()),
                    a.getOrganizationName(),
                    a.getAgentKind(),
                    a.getFirstName(),
                    a.getLastName(),
                    a.getEmail(),
                    a.getPhoneNumber(),
                    a.getAgentType(),
                    a.getRole(),
                    str(a.getBranchId()),
                    String.valueOf(a.isActive()),
                    str(a.getLocationId()),
                    a.getAssignedRegion()
            });
        }
        return toBytes("Agents", "AGENTS", AGENT_HEADERS, rows, format);
    }

    // =========================================================
    // Industry
    // =========================================================

    public byte[] industriesToBytes(List<IndustryDto> items, String format) throws IOException, DocumentException {
        List<String[]> rows = new ArrayList<>();
        for (IndustryDto i : items) {
            rows.add(new String[]{
                    str(i.getId()),
                    i.getName(),
                    i.getIndustryCode(),
                    i.getDescription(),
                    i.getRegulatoryBodyName(),
                    i.getRegulatoryBodyContactInfo(),
                    i.getComplianceRequirements(),
                    String.valueOf(i.isActive())
            });
        }
        return toBytes("Industries", "INDUSTRIES", INDUSTRY_HEADERS, rows, format);
    }

    // =========================================================
    // Private helpers
    // =========================================================

    private byte[] toBytes(String title, String reportCode, String[] headers,
                           List<String[]> rows, String format) throws IOException, DocumentException {
        if (LdmsExportMediaTypes.isPdf(format)) {
            LdmsExportReport report = LdmsExportReport.builder()
                    .title(title).reportCode(reportCode).columnHeaders(headers).rows(rows).build();
            return LdmsPdfReportWriter.write(report);
        }
        if (LdmsExportMediaTypes.isExcel(format)) {
            LdmsExportReport report = LdmsExportReport.builder()
                    .title(title).reportCode(reportCode).columnHeaders(headers).rows(rows).build();
            return LdmsExcelReportWriter.write(title, report);
        }
        // Default: CSV
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(headers);
            rows.forEach(writer::writeNext);
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
