package projectlx.messaging.inbound.business.logic.support;

import projectlx.messaging.inbound.utils.enums.BotAssistantMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import projectlx.messaging.inbound.model.BotMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class BotAgentOrchestrator {

    private static final int MAX_TOOL_ITERATIONS = 6;

    private final BotAgentLlmBridge agentLlmBridge;
    private final BotAgentToolRegistry toolRegistry;
    private final BotLlmClient assistantLlmClient;
    private final BotGuideModeSupport botGuideModeSupport;
    private final ObjectMapper objectMapper;

    public BotAgentOrchestrator(BotAgentLlmBridge agentLlmBridge,
                                BotAgentToolRegistry toolRegistry,
                                BotLlmClient assistantLlmClient,
                                BotGuideModeSupport botGuideModeSupport,
                                ObjectMapper objectMapper) {
        this.agentLlmBridge = agentLlmBridge;
        this.toolRegistry = toolRegistry;
        this.assistantLlmClient = assistantLlmClient;
        this.botGuideModeSupport = botGuideModeSupport;
        this.objectMapper = objectMapper;
    }

    public String run(String systemPrompt,
                      List<BotMessage> history,
                      BotAgentExecutionContext context) {
        if (!agentLlmBridge.isConfigured()) {
            if (botGuideModeSupport != null) {
                return BotLlmHistorySupport.stripLegacyKeyNag(
                        botGuideModeSupport.agentReply(history, context));
            }
            return BotLlmHistorySupport.stripLegacyKeyNag(
                    assistantLlmClient.generateReply(systemPrompt, history));
        }

        boolean anthropic = agentLlmBridge.usesAnthropic();
        ArrayNode conversation = anthropic
                ? agentLlmBridge.initialConversation(history)
                : agentLlmBridge.geminiInitialConversation(history);

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            BotAssistantMode mode = context.assistantMode() != null
                    ? context.assistantMode() : BotAssistantMode.ASSISTANT;
            BotAgentTurnResult turn = agentLlmBridge.generateAgentTurn(systemPrompt, conversation, toolRegistry, mode);
            List<BotAgentToolCall> toolCalls = turn.toolCalls();
            if (toolCalls.isEmpty() && mode == BotAssistantMode.AGENT) {
                toolCalls = BotAgentTextToolCallSupport.parseFromText(turn.text(), objectMapper);
            }
            if (toolCalls.isEmpty()) {
                String reply = BotResponseSanitizer.forUserDisplay(turn.text());
                if (!reply.isBlank()) {
                    return reply;
                }
                break;
            }

            BotAgentTurnResult executableTurn = toolCalls == turn.toolCalls()
                    ? turn
                    : new BotAgentTurnResult("", toolCalls);
            List<String> toolResults = executeTools(executableTurn.toolCalls(), context);
            if (anthropic) {
                agentLlmBridge.appendAnthropicAssistantTurn(conversation, executableTurn);
                agentLlmBridge.appendAnthropicToolResults(conversation, executableTurn.toolCalls(), toolResults);
            } else {
                agentLlmBridge.appendGeminiFunctionCall(conversation, executableTurn);
                agentLlmBridge.appendGeminiFunctionResponses(conversation, executableTurn.toolCalls(), toolResults);
            }
        }

        String fallback = BotResponseSanitizer.forUserDisplay(
                assistantLlmClient.generateReply(systemPrompt, history));
        return fallback.isBlank()
                ? "I pulled some information but couldn't finish — try again or ask me to open a support ticket."
                : fallback;
    }

    private List<String> executeTools(List<BotAgentToolCall> calls, BotAgentExecutionContext context) {
        List<String> results = new ArrayList<>();
        BotAssistantMode mode = context.assistantMode() != null
                ? context.assistantMode() : BotAssistantMode.ASSISTANT;
        for (BotAgentToolCall call : calls) {
            BotAgentTool tool = toolRegistry.findForMode(call.name(), mode);
            if (tool == null) {
                BotAgentTool blocked = toolRegistry.find(call.name());
                if (blocked != null && blocked.mutating()) {
                    results.add("Tool " + call.name()
                            + " requires **Agent** mode (toggle Agent in the toolbar). "
                            + "It creates or changes data in LDMS.");
                } else {
                    results.add("Unknown tool: " + call.name());
                }
                continue;
            }
            try {
                results.add(tool.execute(context, call.input()));
            } catch (Exception ex) {
                log.warn("Agent tool {} failed: {}", call.name(), ex.getMessage());
                results.add("Tool " + call.name() + " failed: " + ex.getMessage());
            }
        }
        return results;
    }

    public static BotAgentExecutionContext contextFrom(String username,
                                                       BotCallerProfileSupport.CallerProfile profile,
                                                       Locale locale,
                                                       BotAssistantMode assistantMode) {
        return new BotAgentExecutionContext(
                username,
                profile.organizationId(),
                profile.organizationClassification(),
                locale == null ? Locale.ENGLISH : locale,
                assistantMode != null ? assistantMode : BotAssistantMode.ASSISTANT);
    }
}
