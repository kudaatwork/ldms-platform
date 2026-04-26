package projectlx.user.management.utils.formatter;

public class PhoneNumberService {

    public String formatPhoneNumber(String phoneNumber) {
        // Remove all non-digit characters except +
        String cleaned = phoneNumber.replaceAll("[^+\\d]", "");

        // Zimbabwe country code validation
        if (cleaned.startsWith("0771") || cleaned.startsWith("0772") ||
                cleaned.startsWith("0773") || cleaned.startsWith("0774") ||
                cleaned.startsWith("0775") || cleaned.startsWith("0776") ||
                cleaned.startsWith("0777") || cleaned.startsWith("0778")) {
            // Remove leading 0 and add country code
            cleaned = "+263" + cleaned.substring(1);
        } else if (cleaned.startsWith("771") || cleaned.startsWith("772") ||
                cleaned.startsWith("773") || cleaned.startsWith("774") ||
                cleaned.startsWith("775") || cleaned.startsWith("776") ||
                cleaned.startsWith("777") || cleaned.startsWith("778")) {
            // Add country code
            cleaned = "+263" + cleaned;
        } else if (!cleaned.startsWith("+263")) {
            throw new IllegalArgumentException("Invalid Zimbabwe phone number: " + phoneNumber);
        }

        return cleaned;
    }

    public boolean isValidZimbabweNumber(String phoneNumber) {
        String formatted = formatPhoneNumber(phoneNumber);
        return formatted.matches("\\+263(77[1-8]|71[1-9]|73[0-9]|78[0-9])\\d{6}");
    }
}
