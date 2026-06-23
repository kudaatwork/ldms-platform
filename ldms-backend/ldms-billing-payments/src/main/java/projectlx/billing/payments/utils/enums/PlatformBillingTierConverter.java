package projectlx.billing.payments.utils.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Tolerates legacy and new {@code billing_tier} values on {@code platform_action_charge}.
 */
@Converter(autoApply = false)
public class PlatformBillingTierConverter implements AttributeConverter<PlatformBillingTier, String> {

    @Override
    public String convertToDatabaseColumn(PlatformBillingTier attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public PlatformBillingTier convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return PlatformBillingTier.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
