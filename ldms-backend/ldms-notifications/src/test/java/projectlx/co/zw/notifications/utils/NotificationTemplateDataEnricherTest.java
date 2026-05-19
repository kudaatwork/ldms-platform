package projectlx.co.zw.notifications.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotificationTemplateDataEnricherTest {

    @Test
    void enrich_usesFirstNameWhenPresent() {
        Map<String, Object> enriched = NotificationTemplateDataEnricher.enrich(Map.of(
                "firstName", "Tariro",
                "userName", "tariro.ncube"));

        assertEquals("Tariro", enriched.get("firstName"));
        assertEquals("tariro.ncube", enriched.get("userName"));
    }

    @Test
    void enrich_fallsBackToUsernameWhenFirstNameMissing() {
        Map<String, Object> enriched = NotificationTemplateDataEnricher.enrich(Map.of(
                "userName", "tariro.ncube"));

        assertEquals("tariro.ncube", enriched.get("firstName"));
        assertEquals("tariro.ncube", enriched.get("userName"));
    }

    @Test
    void enrich_fallsBackToUsernameWhenFirstNameBlank() {
        Map<String, Object> enriched = NotificationTemplateDataEnricher.enrich(Map.of(
                "firstName", "   ",
                "userName", "tariro.ncube"));

        assertEquals("tariro.ncube", enriched.get("firstName"));
    }

    @Test
    void enrich_supportsUsernameAliasKey() {
        Map<String, Object> enriched = NotificationTemplateDataEnricher.enrich(Map.of(
                "username", "tariro.ncube"));

        assertEquals("tariro.ncube", enriched.get("firstName"));
        assertEquals("tariro.ncube", enriched.get("userName"));
    }

    @Test
    void enrich_leavesFirstNameUnsetWhenNoIdentityFields() {
        Map<String, Object> enriched = NotificationTemplateDataEnricher.enrich(Map.of(
                "verificationLink", "https://example.com/verify"));

        assertNull(enriched.get("firstName"));
        assertNull(enriched.get("userName"));
    }
}
