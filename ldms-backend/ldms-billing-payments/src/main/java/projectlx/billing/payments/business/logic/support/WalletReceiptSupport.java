package projectlx.billing.payments.business.logic.support;

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
import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.billing.payments.model.PlatformWallet;
import projectlx.billing.payments.model.WalletTransaction;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public final class WalletReceiptSupport {

    private static final DateTimeFormatter RECEIPT_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final Color BRAND_ACCENT = new Color(234, 88, 12);
    private static final Color TEXT_PRIMARY = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color TEXT_FOOTER = new Color(148, 163, 184);
    private static final Color BORDER = new Color(226, 232, 240);

    private WalletReceiptSupport() {
    }

    public static String generateReceiptNumber(Long transactionId) {
        String suffix = transactionId != null ? String.format("%06d", transactionId) : "000000";
        return "LX-RCP-" + java.time.LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
    }

    public static String buildReceiptHtml(
            WalletTransaction tx,
            PlatformWallet wallet,
            OrganizationBillingSetting setting) {
        ReceiptDetails details = resolveReceiptDetails(tx, wallet, setting);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <title>Receipt %s</title>
                  <style>
                    body { font-family: 'Segoe UI', system-ui, sans-serif; color: #0f172a; margin: 40px; }
                    .header { border-bottom: 3px solid #ea580c; padding-bottom: 16px; margin-bottom: 24px; }
                    h1 { margin: 0; font-size: 22px; }
                    .meta { color: #64748b; font-size: 13px; margin-top: 6px; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 20px; }
                    td { padding: 10px 0; border-bottom: 1px solid #e2e8f0; font-size: 14px; }
                    td.label { color: #64748b; width: 40%%; }
                    .total { font-size: 20px; font-weight: 700; color: #ea580c; }
                    .footer { margin-top: 32px; font-size: 12px; color: #94a3b8; }
                  </style>
                </head>
                <body>
                  <div class="header">
                    <h1>Project LX — Wallet receipt</h1>
                    <div class="meta">Receipt %s · %s</div>
                  </div>
                  <table>
                    <tr><td class="label">Organisation</td><td>%s</td></tr>
                    <tr><td class="label">Transaction</td><td>%s</td></tr>
                    <tr><td class="label">Description</td><td>%s</td></tr>
                    <tr><td class="label">Amount</td><td class="total">%s</td></tr>
                    <tr><td class="label">Balance after</td><td>%s</td></tr>
                  </table>
                  <p class="footer">This receipt confirms a platform wallet movement on Project LX. Keep for your records.</p>
                </body>
                </html>
                """.formatted(
                details.receiptNo(),
                details.receiptNo(),
                details.when(),
                details.orgName(),
                details.type(),
                details.description(),
                details.amount(),
                details.balance());
    }

    public static byte[] buildReceiptPdf(
            WalletTransaction tx,
            PlatformWallet wallet,
            OrganizationBillingSetting setting) throws DocumentException {
        ReceiptDetails details = resolveReceiptDetails(tx, wallet, setting);

        Document document = new Document(PageSize.A4, 48, 48, 48, 48);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, TEXT_PRIMARY);
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 11, TEXT_MUTED);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 11, TEXT_MUTED);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 12, TEXT_PRIMARY);
        Font amountFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BRAND_ACCENT);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_FOOTER);

        document.add(new Paragraph("Project LX — Wallet receipt", titleFont));
        document.add(new Paragraph("Receipt " + details.receiptNo() + " · " + details.when(), metaFont));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(new float[]{2.2f, 3.8f});
        table.setWidthPercentage(100);
        addReceiptRow(table, "Organisation", details.orgName(), labelFont, valueFont);
        addReceiptRow(table, "Transaction", details.type(), labelFont, valueFont);
        addReceiptRow(table, "Description", details.description(), labelFont, valueFont);
        addReceiptRow(table, "Amount", details.amount(), labelFont, amountFont);
        addReceiptRow(table, "Balance after", details.balance(), labelFont, valueFont);
        document.add(table);

        document.add(new Paragraph(" "));
        document.add(new Paragraph(
                "This receipt confirms a platform wallet movement on Project LX. Keep for your records.",
                footerFont));

        document.close();
        return out.toByteArray();
    }

    private static void addReceiptRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorderColor(BORDER);
        labelCell.setPadding(10f);
        labelCell.setBorderWidthBottom(0.5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        valueCell.setBorderColor(BORDER);
        valueCell.setPadding(10f);
        valueCell.setBorderWidthBottom(0.5f);
        table.addCell(valueCell);
    }

    private static ReceiptDetails resolveReceiptDetails(
            WalletTransaction tx,
            PlatformWallet wallet,
            OrganizationBillingSetting setting) {
        String orgName = wallet != null && wallet.getOrganizationName() != null
                ? wallet.getOrganizationName()
                : (setting != null ? setting.getOrganizationName() : "Organisation");
        String currency = wallet != null && wallet.getCurrencyCode() != null ? wallet.getCurrencyCode() : "USD";
        long amountCents = tx.getAmountCents() != null ? tx.getAmountCents() : 0L;
        String amount = formatMoney(amountCents, currency);
        String balance = formatMoney(tx.getBalanceAfterCents() != null ? tx.getBalanceAfterCents() : 0L, currency);
        String receiptNo = tx.getReceiptNumber() != null ? tx.getReceiptNumber() : generateReceiptNumber(tx.getId());
        String when = tx.getCreatedAt() != null ? tx.getCreatedAt().format(RECEIPT_DATE) : "";
        String type = tx.getTransactionType() != null ? tx.getTransactionType().name() : "TRANSACTION";
        String description = tx.getDescription() != null ? tx.getDescription() : type;
        return new ReceiptDetails(receiptNo, when, orgName, type, description, amount, balance);
    }

    private record ReceiptDetails(
            String receiptNo,
            String when,
            String orgName,
            String type,
            String description,
            String amount,
            String balance) {
    }

    private static String formatMoney(long cents, String currency) {
        BigDecimal major = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return currency + " " + major.toPlainString();
    }
}
