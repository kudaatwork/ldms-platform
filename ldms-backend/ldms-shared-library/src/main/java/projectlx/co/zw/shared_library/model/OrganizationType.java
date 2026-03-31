package projectlx.co.zw.shared_library.model;

public enum OrganizationType {
    PRIVATE("PRIVATE"),
    GOVERNMENT("GOVERNMENT"),
    NGO("NGO"),
    NON_PROFIT("NON_PROFIT"),
    PUBLIC("PUBLIC"),
    COOPERATIVE("COOPERATIVE"),
    OTHER("OTHER");

    private final String description;

    OrganizationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
