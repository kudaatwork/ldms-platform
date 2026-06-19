package projectlx.co.zw.shared_library.utils.enums;

/**
 * Determines how an organisation sources inventory data for its logistics operations.
 * <ul>
 *   <li>{@link #INTERNAL} – stock is managed fully within the LDMS Inventory module.</li>
 *   <li>{@link #EXTERNAL_API} – inventory quantities and product catalogue are pulled from a
 *       third-party ERP or WMS via integration hooks.</li>
 *   <li>{@link #MANUAL_ACK} – operators manually acknowledge shipment items; no real-time stock
 *       sync is maintained.</li>
 * </ul>
 */
public enum InventoryDataSource {
    INTERNAL,
    EXTERNAL_API,
    MANUAL_ACK
}
