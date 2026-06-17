package projectlx.messaging.inbound.business.logic.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Slf4j
public class GeminiLlmClient {

    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final HttpClient httpClient;

    public GeminiLlmClient(ObjectMapper objectMapper, String apiKey, String model, boolean enabled) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gemini-2.5-flash" : model.trim();
        this.enabled = enabled;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public String generateReply(String systemPrompt, List<BotMessage> history) {
        if (!enabled || apiKey.isBlank()) {
            return fallbackReply(lastUserMessage(history));
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            ObjectNode systemInstruction = body.putObject("systemInstruction");
            ArrayNode systemParts = systemInstruction.putArray("parts");
            systemParts.addObject().put("text", systemPrompt);

            ArrayNode contents = body.putArray("contents");
            for (BotMessage message : history) {
                if (message.getRole() == BotMessageRole.SYSTEM) {
                    continue;
                }
                ObjectNode turn = contents.addObject();
                turn.put("role", message.getRole() == BotMessageRole.BOT ? "model" : "user");
                turn.putArray("parts").addObject().put("text", message.getBody());
            }

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini API returned HTTP {}: {}", response.statusCode(), truncate(response.body(), 300));
                return fallbackReply(lastUserMessage(history));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return fallbackReply(lastUserMessage(history));
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                return fallbackReply(lastUserMessage(history));
            }
            String text = parts.get(0).path("text").asText("").trim();
            return text.isBlank() ? fallbackReply(lastUserMessage(history)) : text;
        } catch (Exception ex) {
            log.error("Gemini generateContent failed", ex);
            return fallbackReply(lastUserMessage(history));
        }
    }

    private static String lastUserMessage(List<BotMessage> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            BotMessage message = history.get(i);
            if (message.getRole() == BotMessageRole.USER && message.getBody() != null) {
                return message.getBody();
            }
        }
        return "";
    }

    private String fallbackReply(String userMessage) {
        String lower = userMessage == null ? "" : userMessage.toLowerCase();
        if (lower.contains("shipment") || lower.contains("dispatch")) {
            return "In LDMS, shipments are created after a purchase order is approved and stock is dispatched. "
                    + "Track them from **Track shipments** or your operations dashboard. "
                    + "For a specific reference, open Help & Support → New ticket with the shipment number.";
        }
        if (lower.contains("trip") || lower.contains("track") || lower.contains("gps")) {
            return "Trips start when a truck and driver are assigned to a shipment. Live GPS and stop events "
                    + "are recorded in **Trip & Tracking**. Drivers can also report border stops via WhatsApp commands.";
        }
        if (lower.contains("invoice") || lower.contains("billing") || lower.contains("payment")) {
            return "Invoices are generated after goods are received (GRV). View them under **Billing** in the platform portal. "
                    + "For disputes, open a **Billing** support ticket with the invoice number.";
        }
        if (lower.contains("kyc") || lower.contains("onboard") || lower.contains("register")) {
            return "Organisation onboarding includes registration, email verification, and KYC review. "
                    + "After approval, your contact person receives sign-in credentials. "
                    + "See Help & Support → FAQ → Getting started for the full flow.";
        }
        if (lower.contains("grv") || lower.contains("deliver")) {
            return "A GRV (Goods Received Voucher) confirms delivery at the destination. Receivers scan QR codes "
                    + "or confirm in the Receiver app. This triggers invoicing in the billing phase.";
        }
        return "I'm the LDMS assistant. I can explain how orders, shipments, trips, billing, and onboarding work. "
                + "Ask a specific question about LDMS, or open **Help & Support → New ticket** for human help. "
                + "(Configure GEMINI_API_KEY for full AI answers.)";
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
