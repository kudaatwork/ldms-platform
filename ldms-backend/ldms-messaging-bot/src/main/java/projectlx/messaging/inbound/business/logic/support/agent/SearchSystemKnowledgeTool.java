package projectlx.messaging.inbound.business.logic.support.agent;

import projectlx.messaging.inbound.business.logic.support.BotAgentExecutionContext;
import projectlx.messaging.inbound.business.logic.support.BotAgentTool;
import projectlx.messaging.inbound.business.logic.support.BotAgentToolRegistry;
import projectlx.messaging.inbound.business.logic.support.BotFaqRagSupport;
import projectlx.messaging.inbound.business.logic.support.BotKnowledgeDocumentRagSupport;
import projectlx.messaging.inbound.business.logic.support.LdmsKnowledgeContextSupport;

import java.util.LinkedHashMap;
import java.util.Map;

public class SearchSystemKnowledgeTool implements BotAgentTool {

    private final LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport;
    private final BotFaqRagSupport botFaqRagSupport;
    private final BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport;

    public SearchSystemKnowledgeTool(LdmsKnowledgeContextSupport ldmsKnowledgeContextSupport,
                                     BotFaqRagSupport botFaqRagSupport,
                                     BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport) {
        this.ldmsKnowledgeContextSupport = ldmsKnowledgeContextSupport;
        this.botFaqRagSupport = botFaqRagSupport;
        this.botKnowledgeDocumentRagSupport = botKnowledgeDocumentRagSupport;
    }

    @Override
    public String name() {
        return "search_system_knowledge";
    }

    @Override
    public String description() {
        return "Search LDMS architecture docs, agent capabilities, admin FAQ, and uploaded knowledge for service ownership, events, and workflows.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("query", BotAgentToolRegistry.stringParam("Topic or keywords to search (e.g. purchase order approval, trip.started).", true));
        return schema;
    }

    @Override
    public String execute(BotAgentExecutionContext context, Map<String, Object> arguments) {
        String query = BotAgentToolRegistry.readString(arguments, "query");
        if (query.isBlank()) {
            return "query is required.";
        }
        StringBuilder out = new StringBuilder();
        String docContext = botKnowledgeDocumentRagSupport.retrieveContextForQuery(query);
        if (!docContext.isBlank()) {
            out.append("Uploaded knowledge:\n").append(docContext).append("\n\n");
        }
        String faqContext = botFaqRagSupport.retrieveContextForQuery(query);
        if (!faqContext.isBlank()) {
            out.append("Admin FAQ:\n").append(faqContext).append("\n\n");
        }
        String reference = ldmsKnowledgeContextSupport.searchReferenceKnowledge(query);
        if (!reference.isBlank()) {
            out.append("LDMS reference:\n").append(reference);
        }
        if (out.isEmpty()) {
            return "No matching LDMS knowledge found for: " + query;
        }
        return out.toString().trim();
    }
}
