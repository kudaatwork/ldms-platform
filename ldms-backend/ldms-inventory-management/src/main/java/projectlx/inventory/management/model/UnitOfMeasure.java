package projectlx.inventory.management.model;

import java.util.Locale;
import java.util.Map;

public enum UnitOfMeasure {
    EACH("EACH"),
    BOX("BOX"),
    PACK("PACK"),
    KILOGRAM("KILOGRAM"),
    GRAM("GRAM"),
    LITER("LITER"),
    MILLILITER("MILLILITER"),
    METER("METER"),
    CENTIMETER("CENTIMETER"),
    CYLINDER("CYLINDER"),
    CUBIC_METER("CUBIC_METER"),
    SERVICE("SERVICE");

    private final String description;

    UnitOfMeasure(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    private static final Map<String, UnitOfMeasure> CSV_ALIASES = Map.ofEntries(
            Map.entry("UNIT", EACH),
            Map.entry("UNITS", EACH),
            Map.entry("KG", KILOGRAM),
            Map.entry("KGS", KILOGRAM),
            Map.entry("KILO", KILOGRAM),
            Map.entry("G", GRAM),
            Map.entry("L", LITER),
            Map.entry("LITRE", LITER),
            Map.entry("LITRES", LITER),
            Map.entry("LITERS", LITER),
            Map.entry("ML", MILLILITER),
            Map.entry("MILLILITRE", MILLILITER),
            Map.entry("M", METER),
            Map.entry("METRE", METER),
            Map.entry("METRES", METER),
            Map.entry("METERS", METER),
            Map.entry("CM", CENTIMETER),
            Map.entry("CENTIMETRE", CENTIMETER),
            Map.entry("CYL", CYLINDER),
            Map.entry("M3", CUBIC_METER),
            Map.entry("M³", CUBIC_METER),
            Map.entry("CUBIC_METRE", CUBIC_METER),
            Map.entry("CUBIC_METRES", CUBIC_METER)
    );

    /**
     * Resolves spreadsheet / CSV values to a {@link UnitOfMeasure}.
     * Accepts enum names (e.g. {@code CYLINDER}), common aliases ({@code UNIT}, {@code KG}),
     * and ignores surrounding whitespace.
     */
    public static UnitOfMeasure fromCsvValue(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        String key = normalized.toUpperCase(Locale.ROOT).replace(' ', '_');
        UnitOfMeasure alias = CSV_ALIASES.get(key);
        if (alias != null) {
            return alias;
        }
        try {
            return valueOf(key);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
