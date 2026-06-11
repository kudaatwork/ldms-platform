package projectlx.inventory.management.model;

public enum TransactionType {
    STOCK_IN("STOCK_IN"),
    STOCK_OUT("STOCK_OUT"),
    ADJUSTMENT("ADJUSTMENT");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}