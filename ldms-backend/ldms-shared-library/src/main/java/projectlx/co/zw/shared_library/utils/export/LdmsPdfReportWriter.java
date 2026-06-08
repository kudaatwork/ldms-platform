package projectlx.co.zw.shared_library.utils.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Standard LDMS PDF export layout (ISO-style document header, branded table, footer).
 * All services should use this writer for PDF exports going forward.
 */
public final class LdmsPdfReportWriter {

    private static final String LOGO_CLASSPATH = "/export/lx-logo.png";
    private static final Color BRAND_PRIMARY = new Color(30, 58, 138);
    private static final Color BRAND_ACCENT = new Color(59, 130, 246);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color ZEBRA = new Color(248, 250, 252);
    private static final Color HIGHLIGHT_WARNING = new Color(254, 243, 199);
    private static final Color HIGHLIGHT_DANGER = new Color(254, 226, 226);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final DateTimeFormatter DOC_ID_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

    private LdmsPdfReportWriter() {
    }

    public static byte[] write(LdmsExportReport report) throws DocumentException {
        Rectangle pageSize = report.isLandscape() ? PageSize.A4.rotate() : PageSize.A4;
        Document document = new Document(pageSize, 36, 36, 52, 48);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, out);
        String documentId = buildDocumentId(report.getReportCode());
        writer.setPageEvent(new LdmsPdfFooterEvent(documentId));
        document.open();

        document.add(buildHeaderTable(report, documentId));
        document.add(spacer(6f));
        document.add(buildMetadataBlock(report, documentId));
        document.add(spacer(8f));
        document.add(buildDataTable(report));

        document.close();
        return out.toByteArray();
    }

    private static PdfPTable buildHeaderTable(LdmsExportReport report, String documentId) throws DocumentException {
        PdfPTable header = new PdfPTable(new float[]{1.4f, 3.6f});
        header.setWidthPercentage(100);
        header.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        header.getDefaultCell().setPadding(0);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setPaddingRight(12f);
        Image logo = loadLogo();
        if (logo != null) {
            logo.scaleToFit(120f, 36f);
            logoCell.addElement(logo);
        }
        header.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BRAND_PRIMARY);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);
        Paragraph title = new Paragraph(report.getTitle(), titleFont);
        title.setSpacingAfter(2f);
        titleCell.addElement(title);

        String subtitle = report.getSubtitle();
        if (subtitle != null && !subtitle.isBlank()) {
            Paragraph sub = new Paragraph(subtitle, subtitleFont);
            sub.setSpacingAfter(2f);
            titleCell.addElement(sub);
        }

        Paragraph ref = new Paragraph("Document ref: " + documentId, subtitleFont);
        titleCell.addElement(ref);
        header.addCell(titleCell);

        PdfPCell ruleCell = new PdfPCell();
        ruleCell.setColspan(2);
        ruleCell.setBorder(Rectangle.NO_BORDER);
        ruleCell.setPaddingTop(8f);
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell bar = new PdfPCell();
        bar.setFixedHeight(3f);
        bar.setBackgroundColor(BRAND_ACCENT);
        bar.setBorder(Rectangle.NO_BORDER);
        rule.addCell(bar);
        ruleCell.addElement(rule);
        header.addCell(ruleCell);

        return header;
    }

    private static Paragraph buildMetadataBlock(LdmsExportReport report, String documentId) {
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, TEXT_MUTED);
        Font metaBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, BRAND_PRIMARY);
        Paragraph block = new Paragraph();
        block.setSpacingAfter(2f);
        block.add(new Phrase("Generated: ", metaBold));
        block.add(new Phrase(GENERATED_AT.format(LocalDateTime.now(ZoneOffset.UTC)) + "  |  ", metaFont));
        block.add(new Phrase("Records: ", metaBold));
        block.add(new Phrase(String.valueOf(report.getRows().size()) + "  |  ", metaFont));
        block.add(new Phrase("Classification: ", metaBold));
        block.add(new Phrase("Internal Use", metaFont));
        return block;
    }

    private static PdfPTable buildDataTable(LdmsExportReport report) throws DocumentException {
        String[] headers = report.getColumnHeaders();
        int columns = Math.max(1, headers.length);
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingBefore(2f);
        table.setHeaderRows(1);

        float[] widths = new float[columns];
        for (int i = 0; i < columns; i++) {
            widths[i] = 1f;
        }
        table.setWidths(widths);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, Color.WHITE);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(30, 41, 59));

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            styleHeaderCell(cell);
            table.addCell(cell);
        }

        List<String[]> rows = report.getRows();
        List<LdmsExportRowHighlight> rowHighlights = report.getRowHighlights();
        boolean useHighlights = rowHighlights != null
                && rowHighlights.size() == rows.size()
                && rowHighlights.stream().anyMatch(h -> h != LdmsExportRowHighlight.NONE);
        for (int r = 0; r < rows.size(); r++) {
            String[] row = rows.get(r);
            boolean zebra = !useHighlights && r % 2 == 1;
            LdmsExportRowHighlight highlight = useHighlights ? rowHighlights.get(r) : LdmsExportRowHighlight.NONE;
            for (int c = 0; c < columns; c++) {
                String value = row != null && c < row.length ? nullToEmpty(row[c]) : "";
                PdfPCell cell = new PdfPCell(new Phrase(truncate(value, 512), bodyFont));
                styleBodyCell(cell, zebra, highlight);
                table.addCell(cell);
            }
        }

        if (rows.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("No records match the selected filters.", bodyFont));
            empty.setColspan(columns);
            styleBodyCell(empty, false, LdmsExportRowHighlight.NONE);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(12f);
            table.addCell(empty);
        }

        return table;
    }

    private static void styleHeaderCell(PdfPCell cell) {
        cell.setBackgroundColor(BRAND_PRIMARY);
        cell.setPaddingTop(5f);
        cell.setPaddingBottom(5f);
        cell.setPaddingLeft(4f);
        cell.setPaddingRight(4f);
        cell.setBorderColor(BORDER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    }

    private static void styleBodyCell(PdfPCell cell, boolean zebra, LdmsExportRowHighlight highlight) {
        Color background = switch (highlight) {
            case WARNING -> HIGHLIGHT_WARNING;
            case DANGER -> HIGHLIGHT_DANGER;
            default -> zebra ? ZEBRA : Color.WHITE;
        };
        cell.setBackgroundColor(background);
        cell.setPaddingTop(3.5f);
        cell.setPaddingBottom(3.5f);
        cell.setPaddingLeft(4f);
        cell.setPaddingRight(4f);
        cell.setBorderColor(BORDER);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
    }

    private static Image loadLogo() {
        try (InputStream in = LdmsPdfReportWriter.class.getResourceAsStream(LOGO_CLASSPATH)) {
            if (in == null) {
                return null;
            }
            byte[] bytes = in.readAllBytes();
            return Image.getInstance(bytes);
        } catch (IOException | DocumentException ex) {
            return null;
        }
    }

    private static String buildDocumentId(String reportCode) {
        String code = reportCode == null || reportCode.isBlank() ? "EXP" : reportCode.trim().toUpperCase();
        return "LDMS-" + code + "-" + DOC_ID_TIME.format(LocalDateTime.now(ZoneOffset.UTC));
    }

    private static Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(0);
        p.setSpacingAfter(height);
        return p;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }

    private static final class LdmsPdfFooterEvent extends PdfPageEventHelper {
        private final String documentId;
        private final Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, TEXT_MUTED);

        private LdmsPdfFooterEvent(String documentId) {
            this.documentId = documentId;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            float y = document.bottom() - 22;
            String left = "Project LX · LDMS · Confidential — authorized recipients only";
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(left, footerFont),
                    document.left(), y, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(documentId, footerFont),
                    (document.left() + document.right()) / 2, y, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), footerFont),
                    document.right(), y, 0);
        }
    }
}
