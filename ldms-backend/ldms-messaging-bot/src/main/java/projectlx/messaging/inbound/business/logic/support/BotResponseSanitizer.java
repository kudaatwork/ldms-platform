package projectlx.messaging.inbound.business.logic.support;

import java.util.regex.Pattern;

/** Strips internal RAG / system-prompt scaffolding accidentally echoed in bot replies. */
public final class BotResponseSanitizer {

    private static final Pattern PDF_RAG_HEADER = Pattern.compile(
            "(?is)Uploaded PDF knowledge \\(most relevant sections[^)]*\\):\\s*");
    private static final Pattern FAQ_RAG_HEADER = Pattern.compile(
            "(?is)Admin-maintained FAQ knowledge \\(most relevant[^)]*\\):\\s*");
    private static final Pattern REFERENCE_DOCS_HEADER = Pattern.compile(
            "(?is)LDMS reference documents:\\s*---\\s*");
    private static final Pattern PREFER_FOOTER = Pattern.compile(
            "(?is)Prefer these (uploaded knowledge sections|FAQ answers)[^.]*\\.\\s*");
    private static final Pattern NUMBERED_DOC_LINE = Pattern.compile("(?m)^\\d+\\. \\[[^\\]]+\\]\\s*");
    private static final Pattern FAQ_QA_BLOCK = Pattern.compile("(?m)^\\d+\\. Q: .+?\\n\\s*A: ");

    private BotResponseSanitizer() {
    }

    public static String forUserDisplay(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        String cleaned = BotLlmHistorySupport.stripLegacyKeyNag(text.trim());
        cleaned = PDF_RAG_HEADER.matcher(cleaned).replaceAll("");
        cleaned = FAQ_RAG_HEADER.matcher(cleaned).replaceAll("");
        cleaned = REFERENCE_DOCS_HEADER.matcher(cleaned).replaceAll("");
        cleaned = PREFER_FOOTER.matcher(cleaned).replaceAll("");
        cleaned = NUMBERED_DOC_LINE.matcher(cleaned).replaceAll("");
        cleaned = FAQ_QA_BLOCK.matcher(cleaned).replaceAll("");
        cleaned = cleaned.replaceAll("(?m)^\\s*Category: [A-Z_]+\\s*$", "");
        cleaned = BotAgentTextToolCallSupport.stripToolMarkup(cleaned);
        return cleaned.replaceAll("\\n{3,}", "\n\n").trim();
    }
}
