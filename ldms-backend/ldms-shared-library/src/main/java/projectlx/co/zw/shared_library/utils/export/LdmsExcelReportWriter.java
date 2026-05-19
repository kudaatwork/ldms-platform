package projectlx.co.zw.shared_library.utils.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Standard LDMS Excel (.xlsx) export for tabular reports.
 */
public final class LdmsExcelReportWriter {

    private LdmsExcelReportWriter() {
    }

    public static byte[] write(String sheetName, LdmsExportReport report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sanitizeSheetName(sheetName));
            String[] headers = report.getColumnHeaders();
            List<String[]> rows = report.getRows();

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 9);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (String[] data : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < headers.length; c++) {
                    String value = data != null && c < data.length && data[c] != null ? data[c] : "";
                    row.createCell(c).setCellValue(value);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int width = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(width + 512, 24_576));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static String sanitizeSheetName(String name) {
        String base = name == null || name.isBlank() ? "Export" : name;
        return base.replaceAll("[\\\\/*?:\\[\\]]", " ").substring(0, Math.min(31, base.length()));
    }
}
