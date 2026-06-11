package projectlx.inventory.management.business.logic.support;

import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Page;
import projectlx.co.zw.shared_library.utils.export.LdmsExportReport;
import projectlx.co.zw.shared_library.utils.export.LdmsPdfReportWriter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Shared helpers for inventory catalogue export (CSV / XLSX / PDF). */
public final class InventoryExportSupport {

    public static final int MAX_FILTER_PAGE_SIZE = 10_000;

    private InventoryExportSupport() {
    }

    public static <T> List<T> itemsFromPage(Page<T> page) {
        return Optional.ofNullable(page).map(Page::getContent).orElse(Collections.emptyList());
    }

    public static <T> List<T> nullSafe(List<T> items) {
        return items != null ? items : Collections.emptyList();
    }

    public static byte[] writeTabularPdf(String title, String reportCode, String subtitle,
                                         String[] columnHeaders, List<String[]> rows,
                                         boolean landscape) throws DocumentException {
        return LdmsPdfReportWriter.write(LdmsExportReport.builder()
                .title(title)
                .reportCode(reportCode)
                .subtitle(subtitle)
                .columnHeaders(columnHeaders)
                .rows(rows != null ? rows : Collections.emptyList())
                .landscape(landscape)
                .build());
    }

    public static <T> List<T> resolveExportItems(Page<T> page, List<T> list) {
        List<T> fromPage = itemsFromPage(page);
        if (!fromPage.isEmpty()) {
            return fromPage;
        }
        return nullSafe(list);
    }
}
