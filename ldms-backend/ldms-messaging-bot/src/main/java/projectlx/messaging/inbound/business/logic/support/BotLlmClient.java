package projectlx.messaging.inbound.business.logic.support;

import projectlx.messaging.inbound.model.BotMessage;

import java.util.List;

/**
 * Pluggable LLM backend for the LDMS assistant (Gemini, Anthropic Claude, etc.).
 * Future agent tooling can extend {@link BotLlmGenerateContext} with tool definitions.
 */
public interface BotLlmClient {

    String providerId();

    String modelId();

    boolean isConfigured();

    String generateReply(String systemPrompt, List<BotMessage> history);
}
