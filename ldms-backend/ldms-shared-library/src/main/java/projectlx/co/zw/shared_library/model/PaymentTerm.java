package projectlx.co.zw.shared_library.model;

public enum PaymentTerm {

    /**
     * Payment due within 30 days after the invoice date.
     */
    NET_30("NET_30"),

    /**
     * Payment due within 60 days after the invoice date.
     */
    NET_60("NET_60"),

    /**
     * Payment due within 90 days after the invoice date.
     */
    NET_90("NET_90"),

    /**
     * Partial or full payment required before delivery or shipment.
     */
    ADVANCE_PAYMENT("ADVANCE_PAYMENT"),

    /**
     * Cash on Delivery. Payment due at the time of delivery.
     */
    COD("COD"),

    /**
     * Payments are triggered at specific, pre-defined project stages.
     */
    MILESTONE_PAYMENTS("MILESTONE_PAYMENTS");

    private final String description;

    PaymentTerm(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
