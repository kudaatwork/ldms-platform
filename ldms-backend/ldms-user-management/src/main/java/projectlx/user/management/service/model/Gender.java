package projectlx.user.management.service.model;

public enum Gender {
    MALE("MALE"),
    FEMALE("FEMALE"),
    PREFER_NOT_TO_SAY("PREFER_NOT_TO_SAY");

    private final String description;

    Gender(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
