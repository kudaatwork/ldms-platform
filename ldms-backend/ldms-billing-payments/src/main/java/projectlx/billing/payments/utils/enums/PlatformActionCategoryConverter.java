package projectlx.billing.payments.utils.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Tolerates new or legacy {@code platform_action_charge.category} values during catalog rollout.
 */
@Converter
public class PlatformActionCategoryConverter implements AttributeConverter<PlatformActionCategory, String> {

    @Override
    public String convertToDatabaseColumn(PlatformActionCategory attribute) {
        return attribute != null ? attribute.name() : PlatformActionCategory.GENERAL.name();
    }

    @Override
    public PlatformActionCategory convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return PlatformActionCategory.GENERAL;
        }
        try {
            return PlatformActionCategory.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PlatformActionCategory.GENERAL;
        }
    }
}
