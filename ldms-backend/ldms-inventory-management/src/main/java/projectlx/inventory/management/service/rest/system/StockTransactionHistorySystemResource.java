package projectlx.inventory.management.service.rest.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import projectlx.inventory.management.service.processor.api.StockTransactionHistoryServiceProcessor;
import projectlx.inventory.management.utils.dtos.StockTransactionHistoryDto;
import projectlx.inventory.management.utils.requests.StockTransactionHistoryMultipleFiltersRequest;
import projectlx.inventory.management.utils.responses.StockTransactionHistoryResponse;
import projectlx.co.zw.shared_library.utils.audit.Auditable;
import projectlx.co.zw.shared_library.utils.constants.Constants;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@CrossOrigin
@RestController
@RequestMapping("/ldms-inventory-management/v1/system/stock-transactions")
@Tag(name = "Stock Transaction History System Resource", description = "Operations related to managing stock transaction history (system)")
@RequiredArgsConstructor
public class StockTransactionHistorySystemResource {

    private final StockTransactionHistoryServiceProcessor stockTransactionHistoryServiceProcessor;
    private static final Logger logger = LoggerFactory.getLogger(StockTransactionHistorySystemResource.class);

    @Auditable(action = "FIND_STOCK_TRANSACTION_HISTORY_BY_ID")
    @GetMapping("/{id}")
    @Operation(summary = "Find stock transaction history by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock transaction history found successfully"),
            @ApiResponse(responseCode = "404", description = "Stock transaction history not found"),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied")
    })
    public ResponseEntity<StockTransactionHistoryResponse> findById(@PathVariable("id") final Long id,
                                                                    @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                                    @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                                    final Locale locale) {
        return ResponseEntity.ok(stockTransactionHistoryServiceProcessor.findById(id, locale, "SYSTEM"));
    }

    @Auditable(action = "FIND_STOCK_TRANSACTION_HISTORY_BY_MULTIPLE_FILTERS")
    @PostMapping("/search")
    @Operation(summary = "Find stock transaction history by multiple filters")
    public ResponseEntity<StockTransactionHistoryResponse> search(@Valid @RequestBody final StockTransactionHistoryMultipleFiltersRequest request,
                                                                  @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                                  @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                                  final Locale locale) {
        return ResponseEntity.ok(stockTransactionHistoryServiceProcessor.findByMultipleFilters(request, "SYSTEM", locale));
    }

    @Auditable(action = "FIND_STOCK_TRANSACTION_HISTORY_BY_ITEM")
    @GetMapping("/by-item/{inventoryItemId}")
    @Operation(summary = "Get stock transaction history for a single item (paged)")
    public ResponseEntity<StockTransactionHistoryResponse> findByItem(@PathVariable("inventoryItemId") final Long inventoryItemId,
                                                                      @RequestParam(name = "page", defaultValue = "0") final int page,
                                                                      @RequestParam(name = "size", defaultValue = "10") final int size,
                                                                      @Parameter(description = Constants.LOCALE_LANGUAGE_NARRATIVE)
                                                                      @RequestHeader(value = Constants.LOCALE_LANGUAGE, defaultValue = Constants.DEFAULT_LOCALE)
                                                                      final Locale locale) {
        StockTransactionHistoryMultipleFiltersRequest filters = new StockTransactionHistoryMultipleFiltersRequest();
        filters.setInventoryItemId(inventoryItemId);
        filters.setPage(page);
        filters.setSize(size);
        return ResponseEntity.ok(stockTransactionHistoryServiceProcessor.findByMultipleFilters(filters, "SYSTEM", locale));
    }

    @Auditable(action = "EXPORT_STOCK_TRANSACTION_HISTORY_TO_CSV")
    @PostMapping("/export/csv")
    @Operation(summary = "Export provided stock transaction history to CSV")
    public ResponseEntity<byte[]> exportCsv(@RequestBody final List<StockTransactionHistoryDto> items) {
        try {
            byte[] data = stockTransactionHistoryServiceProcessor.exportToCsv(items);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock_transaction_history.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(data);
        } catch (Exception e) {
            String errorMsg = "Failed to export stock transaction history to CSV: " + e.getMessage();
            logger.error(errorMsg, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorMsg.getBytes(StandardCharsets.UTF_8));
        }
    }
}
