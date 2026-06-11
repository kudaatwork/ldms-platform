package projectlx.inventory.management.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Number Generator Utility
 * 
 * Generates unique document numbers for various entities.
 * 
 * FORMAT: {PREFIX}-{YYYYMMDD-HHmmss}-{SUFFIX}
 * Example: PO-20251114-143022-1234
 * 
 * COMPONENTS:
 * - PREFIX: Entity type (PO, SO, GRV, etc.)
 * - DATE-TIME: Timestamp (yyyyMMdd-HHmmss)
 * - SUFFIX: Last 4 digits of current millis (for uniqueness)
 * 
 * This matches your existing number generation pattern from:
 * - PurchaseOrderServiceImpl.generatePurchaseOrderNumber()
 * - SalesOrderServiceImpl.generateSalesOrderNumber()
 */
@Component
public class NumberGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * Generates a Purchase Order number
     * Format: PO-20251114-143022-1234
     */
    public String generatePurchaseOrderNumber() {
        return generateNumber("PO");
    }

    /**
     * Generates a Sales Order number
     * Format: SO-20251114-143022-1234
     */
    public String generateSalesOrderNumber() {
        return generateNumber("SO");
    }

    /**
     * Generates a Goods Received Voucher number
     * Format: GRV-20251114-143022-1234
     */
    public String generateGRVNumber() {
        return generateNumber("GRV");
    }

    /**
     * Generates a Stock Transfer number
     * Format: STR-20251114-143022-1234
     */
    public String generateStockTransferNumber() {
        return generateNumber("STR");
    }

    /**
     * Generates a Stock Adjustment number
     * Format: ADJ-20251114-143022-1234
     */
    public String generateStockAdjustmentNumber() {
        return generateNumber("ADJ");
    }

    /**
     * Generic number generator with custom prefix
     * 
     * @param prefix The document type prefix (e.g., "PO", "SO", "GRV")
     * @return Generated number in format: {PREFIX}-{YYYYMMDD-HHmmss}-{SUFFIX}
     */
    public String generateNumber(String prefix) {
        LocalDateTime now = LocalDateTime.now();
        String datePart = now.format(DATE_TIME_FORMATTER);
        String millis = String.valueOf(System.currentTimeMillis());
        String suffix = millis.substring(millis.length() - 4);
        return prefix + "-" + datePart + "-" + suffix;
    }

    /**
     * Generates a custom formatted number
     * Useful for specific business requirements
     * 
     * @param prefix Document type prefix
     * @param customFormat Custom DateTimeFormatter pattern
     * @return Generated number
     */
    public String generateCustomNumber(String prefix, String customFormat) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(customFormat);
        String datePart = now.format(formatter);
        String millis = String.valueOf(System.currentTimeMillis());
        String suffix = millis.substring(millis.length() - 4);
        return prefix + "-" + datePart + "-" + suffix;
    }
}