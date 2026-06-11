package projectlx.inventory.management.model;

public enum ShipMode {
    OAD("OAD"), SEA("SEA"), AIR("AIR"), RAIL("RAIL"), ROAD("ROAD");

    private final String description;

    ShipMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
