package projectlx.messaging.inbound.business.logic.support;

import projectlx.messaging.inbound.business.logic.support.agent.GetPortalNavigationTool;
import projectlx.messaging.inbound.model.BotMessage;
import projectlx.messaging.inbound.utils.enums.BotMessageRole;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Rich built-in replies when no LLM provider is available — RAG snippets, portal paths, and keyword flows.
 */
public class BotGuideModeSupport {

    private final LdmsKnowledgeContextSupport knowledgeContextSupport;
    private final BotFaqRagSupport botFaqRagSupport;
    private final BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport;
    private final GetPortalNavigationTool portalNavigationTool;

    public BotGuideModeSupport(LdmsKnowledgeContextSupport knowledgeContextSupport,
                               BotFaqRagSupport botFaqRagSupport,
                               BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport) {
        this.knowledgeContextSupport = knowledgeContextSupport;
        this.botFaqRagSupport = botFaqRagSupport;
        this.botKnowledgeDocumentRagSupport = botKnowledgeDocumentRagSupport;
        this.portalNavigationTool = new GetPortalNavigationTool();
    }

    public String assistantReply(String userMessage, List<BotMessage> history, boolean llmConfigured) {
        String effective = resolveEffectiveQuery(userMessage, history);
        if (!llmConfigured && BotLlmFallbackSupport.isAccuracyChallengeQuery(effective)) {
            return guideModePrefix(history) + BotLlmFallbackSupport.accuracyChallengeReplyFor();
        }

        String keywordReply = BotLlmFallbackSupport.keywordReplyFor(effective, history, userMessage);
        if (llmConfigured) {
            return keywordReply;
        }

        String enriched = enrichWithKnowledge(effective, keywordReply);
        if (isGenericStub(keywordReply) && !enriched.equals(keywordReply)) {
            return enriched;
        }
        if (isGenericStub(keywordReply)) {
            return guideModePrefix(history) + keywordReply;
        }
        return guideModePrefix(history) + enriched;
    }

    public String agentReply(List<BotMessage> history, BotAgentExecutionContext context) {
        String effective = resolveEffectiveQuery(BotLlmFallbackSupport.lastUserMessage(history), history);
        if (effective.isBlank()) {
            return guideModePrefix(history)
                    + "Lexi needs an AI provider for Agent tool loops. "
                    + "Switch to **Lexi** (Assistant) for built-in workflow help, or ask your admin to configure Gemini/Anthropic.";
        }

        if (matchesOrderIntent(effective)) {
            return guideModePrefix(history) + orderWorkflowReply(context, effective);
        }

        String knowledge = knowledgeContextSupport.searchReferenceKnowledge(effective);
        String faq = botFaqRagSupport != null ? botFaqRagSupport.bestAnswerForQuery(effective) : "";
        if (!faq.isBlank()) {
            return guideModePrefix(history) + faq;
        }
        if (!knowledge.isBlank()) {
            return guideModePrefix(history) + trimForChat(knowledge, 1200);
        }
        return assistantReply(effective, history, false);
    }

    private String orderWorkflowReply(BotAgentExecutionContext context, String query) {
        StringBuilder reply = new StringBuilder();
        reply.append("Here's how to **create a purchase order / requisition** in LDMS:\n\n");
        reply.append("""
                1. Sign in to the **platform portal** with your organisation account.
                2. Open **My Orders** (customers) or **Inventory → Orders workspace** (suppliers).
                3. Choose **Create order** / **New requisition** and fill in product lines, delivery location, and quantities.
                4. Submit — Inventory publishes `po.created`; the supplier approves (`po.approved`) before dispatch becomes a shipment.
                
                **After approval:** track the PO under **My Orders**, then follow the linked shipment when stock is dispatched.
                """);

        String navigation = portalNavigationTool.execute(context, Map.of());
        if (!navigation.contains("unknown") && !navigation.isBlank()) {
            reply.append("\n**Your portal menu:**\n").append(trimForChat(navigation, 600));
        }

        String knowledge = knowledgeContextSupport.searchReferenceKnowledge(
                query + " purchase order requisition po.created");
        if (!knowledge.isBlank()) {
            reply.append("\n\n**From LDMS guides:**\n").append(trimForChat(knowledge, 800));
        }
        return reply.toString().trim();
    }

    private String enrichWithKnowledge(String effectiveQuery, String keywordReply) {
        String faq = botFaqRagSupport != null ? botFaqRagSupport.bestAnswerForQuery(effectiveQuery) : "";
        if (!faq.isBlank()) {
            return faq;
        }

        String docAnswer = botKnowledgeDocumentRagSupport != null
                ? botKnowledgeDocumentRagSupport.bestSnippetForUserReply(effectiveQuery) : "";
        if (!docAnswer.isBlank()) {
            return docAnswer;
        }

        String reference = knowledgeContextSupport.searchReferenceKnowledge(effectiveQuery);
        if (!reference.isBlank() && isGenericStub(keywordReply)) {
            return trimForChat(reference, 1400);
        }
        if (!reference.isBlank()) {
            return keywordReply + "\n\n**Related guide:**\n" + trimForChat(reference, 600);
        }
        return keywordReply;
    }

    private static String resolveEffectiveQuery(String userMessage, List<BotMessage> history) {
        String effective = history == null || history.isEmpty()
                ? userMessage
                : BotLlmHistorySupport.effectiveUserQuery(history);
        if (effective == null || effective.isBlank()) {
            effective = userMessage == null ? "" : userMessage;
        }
        return effective.trim();
    }

    private static boolean matchesOrderIntent(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        return lower.contains("requis")
                || lower.contains("purchase order")
                || lower.contains("procurement")
                || lower.matches(".*\\bpo\\b.*")
                || lower.contains("create order")
                || lower.contains("new order")
                || lower.contains("my orders");
    }

    private static boolean isGenericStub(String reply) {
        if (reply == null || reply.isBlank()) {
            return true;
        }
        return reply.contains("Ask something specific")
                || reply.contains("could you repeat the LDMS topic");
    }

    private static String guideModePrefix(List<BotMessage> history) {
        if (history != null && hasPriorBotReply(history)) {
            return "";
        }
        return LexiBotPersonality.guideModePrefix();
    }

    private static boolean hasPriorBotReply(List<BotMessage> history) {
        int botReplies = 0;
        for (BotMessage message : history) {
            if (message != null && message.getRole() == BotMessageRole.BOT) {
                botReplies++;
            }
        }
        return botReplies > 0;
    }

    private static String trimForChat(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxChars).trim() + "…";
    }
}
