package projectlx.co.zw.shared_library.utils.export;

/**
 * Content types and filenames for LDMS export HTTP responses.
 */
public final class LdmsExportMediaTypes {

    public static final String CSV = "text/csv;charset=UTF-8";
    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String PDF = "application/pdf";

    private LdmsExportMediaTypes() {
    }

    public static String normalizeFormat(String format) {
        if (format == null) {
            return "csv";
        }
        return format.trim().toLowerCase();
    }

    public static boolean isExcel(String format) {
        String f = normalizeFormat(format);
        return "excel".equals(f) || "xlsx".equals(f);
    }

    public static boolean isPdf(String format) {
        return "pdf".equals(normalizeFormat(format));
    }

    public static boolean isCsv(String format) {
        String f = normalizeFormat(format);
        return "csv".equals(f) || f.isEmpty();
    }
}
