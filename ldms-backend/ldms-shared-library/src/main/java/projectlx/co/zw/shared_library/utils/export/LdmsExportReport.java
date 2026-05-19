package projectlx.co.zw.shared_library.utils.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable description of a tabular LDMS export (PDF / Excel).
 */
public final class LdmsExportReport {

    private final String title;
    private final String reportCode;
    private final String subtitle;
    private final String[] columnHeaders;
    private final List<String[]> rows;
    private final boolean landscape;

    private LdmsExportReport(Builder builder) {
        this.title = builder.title;
        this.reportCode = builder.reportCode;
        this.subtitle = builder.subtitle;
        this.columnHeaders = builder.columnHeaders.clone();
        this.rows = List.copyOf(builder.rows);
        this.landscape = builder.landscape;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTitle() {
        return title;
    }

    public String getReportCode() {
        return reportCode;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String[] getColumnHeaders() {
        return columnHeaders.clone();
    }

    public List<String[]> getRows() {
        return rows;
    }

    public boolean isLandscape() {
        return landscape;
    }

    public static final class Builder {
        private String title = "LDMS Export";
        private String reportCode = "EXP";
        private String subtitle = "";
        private String[] columnHeaders = new String[0];
        private final List<String[]> rows = new ArrayList<>();
        private boolean landscape = true;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder reportCode(String reportCode) {
            this.reportCode = reportCode;
            return this;
        }

        public Builder subtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder columnHeaders(String... columnHeaders) {
            this.columnHeaders = columnHeaders != null ? columnHeaders : new String[0];
            return this;
        }

        public Builder rows(List<String[]> rows) {
            this.rows.clear();
            if (rows != null) {
                this.rows.addAll(rows);
            }
            return this;
        }

        public Builder addRow(String... cells) {
            this.rows.add(cells);
            return this;
        }

        public Builder landscape(boolean landscape) {
            this.landscape = landscape;
            return this;
        }

        public LdmsExportReport build() {
            return new LdmsExportReport(this);
        }
    }

    public static List<String[]> emptyRows() {
        return Collections.emptyList();
    }
}
