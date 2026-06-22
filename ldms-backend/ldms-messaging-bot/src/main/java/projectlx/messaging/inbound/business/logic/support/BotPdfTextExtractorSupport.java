package projectlx.messaging.inbound.business.logic.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Extracts plain text from uploaded files.
 * Supports PDF via Apache PDFBox. Falls back to UTF-8 string decode for plain-text content types.
 */
@Slf4j
public class BotPdfTextExtractorSupport {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final int MAX_BYTES = 20 * 1024 * 1024; // 20 MB guard

    /**
     * Extracts readable text from the given multipart file.
     *
     * @param file the uploaded file
     * @return extracted text, never null (may be blank on failure)
     * @throws IOException if the file cannot be read
     */
    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return "";
        }
        if (file.getSize() > MAX_BYTES) {
            log.warn("Uploaded file '{}' exceeds {} MB limit — skipping text extraction",
                    file.getOriginalFilename(), MAX_BYTES / (1024 * 1024));
            return "";
        }

        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("text/")) {
            return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
        }

        if (isPdf(contentType, file.getOriginalFilename())) {
            return extractFromPdf(file);
        }

        log.warn("Unsupported file type '{}' for text extraction — storing without extracted text", contentType);
        return "";
    }

    private static String extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                log.warn("PDF '{}' is encrypted — cannot extract text", file.getOriginalFilename());
                return "";
            }
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text == null ? "" : text.trim();
        }
    }

    private static boolean isPdf(String contentType, String filename) {
        if (PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
            return true;
        }
        return filename != null && filename.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf");
    }
}
