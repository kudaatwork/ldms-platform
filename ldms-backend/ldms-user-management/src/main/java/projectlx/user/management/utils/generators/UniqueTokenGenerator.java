package projectlx.user.management.utils.generators;

import java.util.UUID;

public class UniqueTokenGenerator {

    public static String GenerateVerificationToken() {
        // Generate a random UUID (e.g., "a8b2c3d4-e5f6-7890-1234-567890abcdef")
        UUID uuid = UUID.randomUUID();
        // Return it as a string
        return uuid.toString();
    }
}
