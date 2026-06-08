package projectlx.co.zw.audittrail.utils.export;

import java.util.Locale;
import projectlx.co.zw.shared_library.utils.export.LdmsExportRowHighlight;

/**
 * Maps audited action codes to export emphasis (created = warning, updated/deleted = danger).
 */
public final class AuditActionHighlight {

    public enum Kind {
        CREATE,
        UPDATE,
        DELETE,
        OTHER
    }

    private AuditActionHighlight() {
    }

    public static Kind classify(String action) {
        String code = action != null ? action.trim().toUpperCase(Locale.ROOT) : "";
        if (code.isEmpty()) {
            return Kind.OTHER;
        }
        if (code.startsWith("CREATE_") || "CREATE".equals(code)) {
            return Kind.CREATE;
        }
        if (code.startsWith("UPDATE_") || code.startsWith("EDIT_") || "UPDATE".equals(code) || "EDIT".equals(code)) {
            return Kind.UPDATE;
        }
        if (code.startsWith("DELETE_") || "DELETE".equals(code)) {
            return Kind.DELETE;
        }
        return Kind.OTHER;
    }

    public static String label(Kind kind) {
        if (kind == null) {
            return "";
        }
        return switch (kind) {
            case CREATE -> "Created";
            case UPDATE -> "Updated";
            case DELETE -> "Deleted";
            default -> "";
        };
    }

    public static String label(String action) {
        return label(classify(action));
    }

    public static LdmsExportRowHighlight pdfHighlight(String action) {
        return pdfHighlight(classify(action));
    }

    public static LdmsExportRowHighlight pdfHighlight(Kind kind) {
        if (kind == null) {
            return LdmsExportRowHighlight.NONE;
        }
        return switch (kind) {
            case CREATE -> LdmsExportRowHighlight.WARNING;
            case UPDATE, DELETE -> LdmsExportRowHighlight.DANGER;
            default -> LdmsExportRowHighlight.NONE;
        };
    }
}
