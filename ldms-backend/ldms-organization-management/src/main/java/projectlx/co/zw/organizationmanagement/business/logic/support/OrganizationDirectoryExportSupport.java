package projectlx.co.zw.organizationmanagement.business.logic.support;

import com.lowagie.text.DocumentException;
import projectlx.co.zw.organizationmanagement.utils.dtos.IndustryDto;
import projectlx.co.zw.shared_library.utils.dtos.AgentDto;
import projectlx.co.zw.shared_library.utils.dtos.BranchDto;
import projectlx.co.zw.shared_library.utils.export.LdmsExcelReportWriter;
import projectlx.co.zw.shared_library.utils.export.LdmsExportReport;
import projectlx.co.zw.shared_library.utils.export.LdmsPdfReportWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class OrganizationDirectoryExportSupport {

    private static final String[] BRANCH_HEADERS = {
            "ID", "ORGANIZATION ID", "ORGANIZATION NAME", "BRANCH NAME", "BRANCH CODE", "REGION",
            "EMAIL", "PHONE NUMBER", "HEAD OFFICE", "ACTIVE", "BUSINESS HOURS", "LOCATION ID"
    };

    private static final String[] AGENT_HEADERS = {
            "ID", "ORGANIZATION ID", "ORGANIZATION NAME", "AGENT KIND", "FIRST NAME", "LAST NAME",
            "EMAIL", "PHONE NUMBER", "AGENT TYPE", "ROLE", "BRANCH ID", "ACTIVE"
    };

    private static final String[] INDUSTRY_HEADERS = {
            "ID", "NAME", "INDUSTRY CODE", "DESCRIPTION", "REGULATORY BODY NAME",
            "REGULATORY BODY CONTACT", "COMPLIANCE REQUIREMENTS", "ACTIVE"
    };

    private OrganizationDirectoryExportSupport() {
    }

    public static byte[] exportBranchesToCsv(List<BranchDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", BRANCH_HEADERS)).append('\n');
        for (BranchDto b : items) {
            sb.append(safeLong(b.getId())).append(',')
                    .append(safeLong(b.getOrganizationId())).append(',')
                    .append(safe(b.getOrganizationName())).append(',')
                    .append(safe(b.getBranchName())).append(',')
                    .append(safe(b.getBranchCode())).append(',')
                    .append(safe(b.getRegion())).append(',')
                    .append(safe(b.getEmail())).append(',')
                    .append(safe(b.getPhoneNumber())).append(',')
                    .append(b.isHeadOffice()).append(',')
                    .append(b.isActive()).append(',')
                    .append(safe(b.getBusinessHours())).append(',')
                    .append(safeLong(b.getLocationId())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] exportBranchesToExcel(List<BranchDto> items) throws IOException {
        return LdmsExcelReportWriter.write("Branches", LdmsExportReport.builder()
                .title("Branches")
                .reportCode("ORG-BRN")
                .subtitle("Organisation branches export")
                .columnHeaders(BRANCH_HEADERS)
                .rows(toBranchRows(items))
                .landscape(true)
                .build());
    }

    public static byte[] exportBranchesToPdf(List<BranchDto> items) throws DocumentException {
        return LdmsPdfReportWriter.write(LdmsExportReport.builder()
                .title("Branches")
                .reportCode("ORG-BRN")
                .subtitle("Organisation branches export")
                .columnHeaders(BRANCH_HEADERS)
                .rows(toBranchRows(items))
                .landscape(true)
                .build());
    }

    public static byte[] exportAgentsToCsv(List<AgentDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", AGENT_HEADERS)).append('\n');
        for (AgentDto a : items) {
            sb.append(safeLong(a.getId())).append(',')
                    .append(safeLong(a.getOrganizationId())).append(',')
                    .append(safe(a.getOrganizationName())).append(',')
                    .append(safe(a.getAgentKind())).append(',')
                    .append(safe(a.getFirstName())).append(',')
                    .append(safe(a.getLastName())).append(',')
                    .append(safe(a.getEmail())).append(',')
                    .append(safe(a.getPhoneNumber())).append(',')
                    .append(safe(a.getAgentType())).append(',')
                    .append(safe(a.getRole())).append(',')
                    .append(safeLong(a.getBranchId())).append(',')
                    .append(a.isActive()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] exportAgentsToExcel(List<AgentDto> items) throws IOException {
        return LdmsExcelReportWriter.write("Agents", LdmsExportReport.builder()
                .title("Agents")
                .reportCode("ORG-AGT")
                .subtitle("Organisation agents export")
                .columnHeaders(AGENT_HEADERS)
                .rows(toAgentRows(items))
                .landscape(true)
                .build());
    }

    public static byte[] exportAgentsToPdf(List<AgentDto> items) throws DocumentException {
        return LdmsPdfReportWriter.write(LdmsExportReport.builder()
                .title("Agents")
                .reportCode("ORG-AGT")
                .subtitle("Organisation agents export")
                .columnHeaders(AGENT_HEADERS)
                .rows(toAgentRows(items))
                .landscape(true)
                .build());
    }

    public static byte[] exportIndustriesToCsv(List<IndustryDto> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", INDUSTRY_HEADERS)).append('\n');
        for (IndustryDto i : items) {
            sb.append(safeLong(i.getId())).append(',')
                    .append(safe(i.getName())).append(',')
                    .append(safe(i.getIndustryCode())).append(',')
                    .append(safe(i.getDescription())).append(',')
                    .append(safe(i.getRegulatoryBodyName())).append(',')
                    .append(safe(i.getRegulatoryBodyContactInfo())).append(',')
                    .append(safe(i.getComplianceRequirements())).append(',')
                    .append(i.isActive()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] exportIndustriesToExcel(List<IndustryDto> items) throws IOException {
        return LdmsExcelReportWriter.write("Industries", LdmsExportReport.builder()
                .title("Industries")
                .reportCode("ORG-IND")
                .subtitle("Industry sectors export")
                .columnHeaders(INDUSTRY_HEADERS)
                .rows(toIndustryRows(items))
                .landscape(true)
                .build());
    }

    public static byte[] exportIndustriesToPdf(List<IndustryDto> items) throws DocumentException {
        return LdmsPdfReportWriter.write(LdmsExportReport.builder()
                .title("Industries")
                .reportCode("ORG-IND")
                .subtitle("Industry sectors export")
                .columnHeaders(INDUSTRY_HEADERS)
                .rows(toIndustryRows(items))
                .landscape(true)
                .build());
    }

    private static List<String[]> toBranchRows(List<BranchDto> items) {
        List<String[]> rows = new ArrayList<>();
        for (BranchDto b : items) {
            rows.add(new String[]{
                    safeLong(b.getId()),
                    safeLong(b.getOrganizationId()),
                    safe(b.getOrganizationName()),
                    safe(b.getBranchName()),
                    safe(b.getBranchCode()),
                    safe(b.getRegion()),
                    safe(b.getEmail()),
                    safe(b.getPhoneNumber()),
                    String.valueOf(b.isHeadOffice()),
                    String.valueOf(b.isActive()),
                    safe(b.getBusinessHours()),
                    safeLong(b.getLocationId())
            });
        }
        return rows;
    }

    private static List<String[]> toAgentRows(List<AgentDto> items) {
        List<String[]> rows = new ArrayList<>();
        for (AgentDto a : items) {
            rows.add(new String[]{
                    safeLong(a.getId()),
                    safeLong(a.getOrganizationId()),
                    safe(a.getOrganizationName()),
                    safe(a.getAgentKind()),
                    safe(a.getFirstName()),
                    safe(a.getLastName()),
                    safe(a.getEmail()),
                    safe(a.getPhoneNumber()),
                    safe(a.getAgentType()),
                    safe(a.getRole()),
                    safeLong(a.getBranchId()),
                    String.valueOf(a.isActive())
            });
        }
        return rows;
    }

    private static List<String[]> toIndustryRows(List<IndustryDto> items) {
        List<String[]> rows = new ArrayList<>();
        for (IndustryDto i : items) {
            rows.add(new String[]{
                    safeLong(i.getId()),
                    safe(i.getName()),
                    safe(i.getIndustryCode()),
                    safe(i.getDescription()),
                    safe(i.getRegulatoryBodyName()),
                    safe(i.getRegulatoryBodyContactInfo()),
                    safe(i.getComplianceRequirements()),
                    String.valueOf(i.isActive())
            });
        }
        return rows;
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String safeLong(Long value) {
        return value != null ? String.valueOf(value) : "";
    }
}
