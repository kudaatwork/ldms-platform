package projectlx.user.management.model;

public enum Gender {
    MALE("MALE"),
    FEMALE("FEMALE"),
    NON_BINARY("NON_BINARY"),
    PREFER_NOT_TO_SAY("PREFER_NOT_TO_SAY");

    private final String description;

    Gender(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
