package projectlx.billing.payments.business.logic.support;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
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

        String rowStyle = "padding:13px 0;border-bottom:1px solid #eef2f7;font-size:13px;";
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1"/>
                  <title>Receipt %s</title>
                </head>
                <body style="margin:0;padding:0;background:#0f172a;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;-webkit-font-smoothing:antialiased;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#0f172a;padding:32px 12px;">
                    <tr><td align="center">
                      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;background:#ffffff;border-radius:22px;overflow:hidden;box-shadow:0 28px 70px rgba(2,6,23,0.5);">
                        <tr><td style="background:linear-gradient(135deg,#ea580c 0%%,#f97316 55%%,#fb923c 100%%);padding:34px 40px;">
                          <table role="presentation" width="100%%"><tr>
                            <td style="color:#ffffff;font-size:13px;font-weight:700;letter-spacing:3px;text-transform:uppercase;opacity:0.92;">Project LX Platform</td>
                            <td align="right"><span style="display:inline-block;background:rgba(255,255,255,0.20);color:#ffffff;font-size:11px;font-weight:800;letter-spacing:1px;padding:7px 15px;border-radius:999px;">PAID</span></td>
                          </tr></table>
                          <div style="color:#ffffff;font-size:27px;font-weight:800;margin-top:20px;letter-spacing:-0.5px;">Payment Receipt</div>
                          <div style="color:rgba(255,255,255,0.88);font-size:13px;margin-top:5px;">%s &middot; %s</div>
                        </td></tr>
                        <tr><td style="padding:36px 40px 6px;text-align:center;">
                          <div style="color:#64748b;font-size:12px;font-weight:700;letter-spacing:1.5px;text-transform:uppercase;">Amount received</div>
                          <div style="color:#0f172a;font-size:42px;font-weight:800;margin-top:8px;letter-spacing:-1px;">%s</div>
                        </td></tr>
                        <tr><td style="padding:20px 40px 8px;">
                          <table role="presentation" width="100%%" style="border-collapse:collapse;">
                            <tr><td style="%scolor:#64748b;">Organisation</td><td style="%scolor:#0f172a;font-weight:600;text-align:right;">%s</td></tr>
                            <tr><td style="%scolor:#64748b;">Type</td><td style="%scolor:#0f172a;font-weight:600;text-align:right;">%s</td></tr>
                            <tr><td style="%scolor:#64748b;">Description</td><td style="%scolor:#0f172a;font-weight:600;text-align:right;">%s</td></tr>
                            <tr><td style="padding:13px 0;color:#64748b;font-size:13px;">Balance after</td><td style="padding:13px 0;color:#0f172a;font-weight:700;font-size:13px;text-align:right;">%s</td></tr>
                          </table>
                        </td></tr>
                        <tr><td style="padding:10px 40px 30px;">
                          <div style="background:#f8fafc;border:1px dashed #cbd5e1;border-radius:14px;padding:16px 18px;text-align:center;">
                            <span style="color:#64748b;font-size:11px;letter-spacing:1.5px;text-transform:uppercase;">Receipt Number</span>
                            <div style="color:#0f172a;font-family:'SF Mono',Menlo,Consolas,monospace;font-size:16px;font-weight:700;margin-top:4px;letter-spacing:0.5px;">%s</div>
                          </div>
                        </td></tr>
                        <tr><td style="background:#0f172a;padding:26px 40px;text-align:center;">
                          <div style="color:#e2e8f0;font-size:13px;font-weight:600;">Thank you for using Project LX</div>
                          <div style="color:#64748b;font-size:11px;margin-top:7px;line-height:1.7;">Official receipt of a wallet movement on the Project LX cross-border logistics platform.<br/>Questions? Reach us via <span style="color:#fb923c;">Help &amp; Support</span> in your portal.</div>
                        </td></tr>
                      </table>
                      <div style="color:#475569;font-size:11px;margin-top:18px;">&copy; Project LX &middot; Cross-border logistics, simplified.</div>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                details.receiptNo(),
                details.receiptNo(),
                details.when(),
                details.amount(),
                rowStyle, rowStyle, details.orgName(),
                rowStyle, rowStyle, details.type(),
                rowStyle, rowStyle, details.description(),
                details.balance(),
                details.receiptNo());
    }

    public static byte[] buildReceiptPdf(
            WalletTransaction tx,
            PlatformWallet wallet,
            OrganizationBillingSetting setting) throws DocumentException {
        ReceiptDetails details = resolveReceiptDetails(tx, wallet, setting);

        Color orange = new Color(234, 88, 12);
        Color orangePill = new Color(247, 165, 110);
        Color dark = new Color(15, 23, 42);
        Color primary = new Color(15, 23, 42);
        Color muted = new Color(100, 116, 139);
        Color lightBg = new Color(248, 250, 252);
        Color dash = new Color(203, 213, 225);
        Color divider = new Color(238, 242, 247);
        Color footerTextColor = new Color(226, 232, 240);
        Color cardBorder = new Color(226, 232, 240);
        Color white = Color.WHITE;

        Document document = new Document(PageSize.A4, 0, 0, 0, 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();
        PdfContentByte cb = writer.getDirectContent();

        float cardX = 47f;
        float cardW = 501f;
        float cardRight = cardX + cardW;
        float cardTopY = 800f;
        float cardBottomY = 80f;
        float radius = 18f;
        float headerHeight = 150f;
        float footerHeight = 92f;
        float headerBottomY = cardTopY - headerHeight;
        float footerTopY = cardBottomY + footerHeight;
        float cx = cardX + cardW / 2f;
        float padLeft = cardX + 33f;
        float padRight = cardRight - 33f;

        // Dark footer band (rounded bottom corners) — drawn first so the body overlaps its top.
        cb.roundRectangle(cardX, cardBottomY, cardW, footerHeight + radius, radius);
        cb.setColorFill(dark);
        cb.fill();

        // Orange header band (rounded top corners).
        cb.roundRectangle(cardX, headerBottomY - radius, cardW, headerHeight + radius, radius);
        cb.setColorFill(orange);
        cb.fill();

        // White body — covers the inner rounded overhangs of the header and footer bands.
        cb.rectangle(cardX, footerTopY, cardW, headerBottomY - footerTopY);
        cb.setColorFill(white);
        cb.fill();

        // Card outline.
        cb.setLineWidth(0.8f);
        cb.setColorStroke(cardBorder);
        cb.roundRectangle(cardX, cardBottomY, cardW, cardTopY - cardBottomY, radius);
        cb.stroke();

        // "PAID" pill in the header.
        float pillW = 58f;
        float pillH = 22f;
        float pillX = padRight - pillW;
        float pillY = cardTopY - 52f;
        cb.roundRectangle(pillX, pillY, pillW, pillH, 11f);
        cb.setColorFill(orangePill);
        cb.fill();

        Font eyebrowFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, white);
        Font pillFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, white);
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, white);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, white);
        Font amountLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, muted);
        Font amountFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, primary);
        Font rowLabelFont = FontFactory.getFont(FontFactory.HELVETICA, 12, muted);
        Font rowValueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, primary);
        Font boxLabelFont = FontFactory.getFont(FontFactory.HELVETICA, 9, muted);
        Font boxValueFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 14, primary);
        Font footerTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, footerTextColor);
        Font footerSmallFont = FontFactory.getFont(FontFactory.HELVETICA, 9, muted);

        // Header text.
        drawText(cb, "PROJECT LX PLATFORM", eyebrowFont, Element.ALIGN_LEFT, padLeft, cardTopY - 40f);
        drawText(cb, "PAID", pillFont, Element.ALIGN_CENTER, pillX + pillW / 2f, pillY + 6f);
        drawText(cb, "Payment Receipt", titleFont, Element.ALIGN_LEFT, padLeft, cardTopY - 95f);
        drawText(cb, details.receiptNo() + "  ·  " + details.when(), subtitleFont,
                Element.ALIGN_LEFT, padLeft, cardTopY - 117f);

        // Amount block.
        drawText(cb, "AMOUNT RECEIVED", amountLabelFont, Element.ALIGN_CENTER, cx, 612f);
        drawText(cb, details.amount(), amountFont, Element.ALIGN_CENTER, cx, 575f);

        // Detail rows.
        drawRow(cb, "Organisation", details.orgName(), rowLabelFont, rowValueFont, divider, padLeft, padRight, 525f);
        drawRow(cb, "Type", details.type(), rowLabelFont, rowValueFont, divider, padLeft, padRight, 487f);
        drawRow(cb, "Description", details.description(), rowLabelFont, rowValueFont, divider, padLeft, padRight, 449f);
        drawRow(cb, "Balance after", details.balance(), rowLabelFont, rowValueFont, divider, padLeft, padRight, 411f);

        // Receipt-number box (dashed border).
        float boxX = padLeft;
        float boxW = padRight - padLeft;
        float boxY = 300f;
        float boxH = 62f;
        cb.roundRectangle(boxX, boxY, boxW, boxH, 12f);
        cb.setColorFill(lightBg);
        cb.fill();
        cb.setLineWidth(1f);
        cb.setColorStroke(dash);
        cb.setLineDash(4f, 3f, 0f);
        cb.roundRectangle(boxX, boxY, boxW, boxH, 12f);
        cb.stroke();
        cb.setLineDash(0f);
        drawText(cb, "RECEIPT NUMBER", boxLabelFont, Element.ALIGN_CENTER, cx, boxY + 40f);
        drawText(cb, details.receiptNo(), boxValueFont, Element.ALIGN_CENTER, cx, boxY + 18f);

        // Footer text.
        drawText(cb, "Thank you for using Project LX", footerTitleFont, Element.ALIGN_CENTER, cx, 140f);
        drawText(cb, "Official receipt of a wallet movement on the Project LX platform.",
                footerSmallFont, Element.ALIGN_CENTER, cx, 122f);
        drawText(cb, "Questions? Reach us via Help & Support in your portal.",
                footerSmallFont, Element.ALIGN_CENTER, cx, 108f);

        document.close();
        return out.toByteArray();
    }

    private static void drawRow(
            PdfContentByte cb,
            String label,
            String value,
            Font labelFont,
            Font valueFont,
            Color divider,
            float xLeft,
            float xRight,
            float y) {
        drawText(cb, label, labelFont, Element.ALIGN_LEFT, xLeft, y);
        drawText(cb, value, valueFont, Element.ALIGN_RIGHT, xRight, y);
        cb.setLineWidth(0.7f);
        cb.setColorStroke(divider);
        cb.moveTo(xLeft, y - 12f);
        cb.lineTo(xRight, y - 12f);
        cb.stroke();
    }

    private static void drawText(
            PdfContentByte cb,
            String value,
            Font font,
            int alignment,
            float x,
            float y) {
        ColumnText.showTextAligned(cb, alignment, new Phrase(value != null ? value : "", font), x, y, 0);
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
