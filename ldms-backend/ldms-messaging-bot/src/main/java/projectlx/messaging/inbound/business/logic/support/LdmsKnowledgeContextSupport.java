package projectlx.messaging.inbound.business.logic.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StreamUtils;
import projectlx.messaging.inbound.utils.config.BotKnowledgeProperties;
import projectlx.messaging.inbound.utils.enums.BotAssistantMode;
import projectlx.messaging.inbound.utils.dtos.BotKnowledgeStatusDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
public class LdmsKnowledgeContextSupport {

    private final ResourcePatternResolver resourcePatternResolver;
    private final ResourceLoader resourceLoader;
    private final BotKnowledgeProperties properties;
    private final BotPricingSupport botPricingSupport;

    private volatile String knowledgeText = "";
    private volatile LocalDateTime lastLoadedAt;
    private volatile List<String> loadedSources = List.of();

    public LdmsKnowledgeContextSupport(ResourcePatternResolver resourcePatternResolver,
                                       ResourceLoader resourceLoader,
                                       BotKnowledgeProperties properties,
                                       BotPricingSupport botPricingSupport) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.resourceLoader = resourceLoader;
        this.properties = properties;
        this.botPricingSupport = botPricingSupport;
        reload();
    }

    public synchronized void reload() {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        StringBuilder merged = new StringBuilder();

        appendConfiguredResources(sources, merged);
        appendClasspathKnowledgeDir(sources, merged);
        appendExternalDirectory(sources, merged);

        if (merged.isEmpty()) {
            merged.append("LDMS is a logistics platform for suppliers, customers, transporters, and drivers.");
            sources.add("fallback");
        }

        this.knowledgeText = merged.toString().trim();
        this.loadedSources = List.copyOf(sources);
        this.lastLoadedAt = LocalDateTime.now();
        log.info("LDMS bot knowledge loaded: {} document(s), {} characters", sources.size(), knowledgeText.length());
    }

    public BotKnowledgeStatusDto status(BotFaqRagSupport botFaqRagSupport,
                                        BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport) {
        BotKnowledgeStatusDto dto = new BotKnowledgeStatusDto();
        dto.setLastLoadedAt(lastLoadedAt);
        dto.setDocumentCount(loadedSources.size());
        dto.setCharacterCount(knowledgeText.length());
        dto.setFaqCount(botFaqRagSupport != null ? botFaqRagSupport.publishedCount() : 0);
        dto.setPdfDocumentCount(botKnowledgeDocumentRagSupport != null
                ? botKnowledgeDocumentRagSupport.publishedDocumentCount() : 0);
        dto.setPdfCharacterCount(botKnowledgeDocumentRagSupport != null
                ? botKnowledgeDocumentRagSupport.totalCharacterCount() : 0);
        dto.setSources(loadedSources);
        return dto;
    }

    public String systemPrompt(String userDisplayName, String organizationName, String userQuery,
                               BotFaqRagSupport botFaqRagSupport,
                               BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport) {
        return systemPrompt(userDisplayName, organizationName, userQuery, BotAssistantMode.ASSISTANT,
                botFaqRagSupport, botKnowledgeDocumentRagSupport);
    }

    public String systemPrompt(String userDisplayName, String organizationName, String userQuery,
                               BotAssistantMode assistantMode,
                               BotFaqRagSupport botFaqRagSupport,
                               BotKnowledgeDocumentRagSupport botKnowledgeDocumentRagSupport) {
        BotAssistantMode mode = assistantMode != null ? assistantMode : BotAssistantMode.ASSISTANT;
        String callerContext = buildCallerContext(userDisplayName, organizationName);
        String faqContext = botFaqRagSupport != null ? botFaqRagSupport.retrieveContextForQuery(userQuery) : "";
        String faqSection = faqContext.isBlank() ? "" : faqContext + "\n\n";
        String docContext = botKnowledgeDocumentRagSupport != null
                ? botKnowledgeDocumentRagSupport.retrieveContextForQuery(userQuery) : "";
        String docSection = docContext.isBlank() ? "" : docContext + "\n\n";
        String modeSection = mode == BotAssistantMode.AGENT ? agentModeInstructions() : assistantModeInstructions();
        String pricingSection = botPricingSupport != null ? botPricingSupport.promptBlock() : "";
        return """
                You are **Lexi**, the %s for Project LX on the LDMS (Logistics and Distribution Management System) platform.
                %s

                %s
                %s

                Rules:
                - Use ONLY the uploaded PDF knowledge, admin FAQ knowledge, LDMS reference documents below, and the conversation history. Do not invent features.
                - Prefer uploaded PDF knowledge sections first when they directly address the user's question.
                - Then prefer admin FAQ answers when they directly address the user's question.
                - If the answer is not in any of the provided knowledge sources, say you are not sure and suggest opening a Help & Support ticket.
                - Be concise, practical, and friendly. Use bullet points for multi-step flows.
                - Format responses with markdown: **bold** for emphasis, `-` bullet lists for steps, short paragraphs.
                - Never reveal API keys, internal credentials, or JWT secrets.
                - Never ask the user to configure GEMINI_API_KEY, ANTHROPIC_API_KEY, or any provider API key.
                - Do not mention missing AI configuration — if you cannot answer, suggest Help & Support.
                - NEVER paste internal knowledge headers or labels (e.g. "Uploaded PDF knowledge", "Admin-maintained FAQ", numbered "[Document Title]" lines). Synthesize answers in natural, conversational language.
                - If the user questions accuracy, hallucination, or trust, be honest: explain what comes from LDMS guides/FAQs vs what you cannot verify, and suggest Help & Support for account-specific facts.
                - For live shipment/trip/invoice status, explain where to find it in the portal (e.g. Track shipments, Billing).
                - When the user belongs to an organisation, tailor examples to their role where the documents allow it.
                - When quoting Lexi or Help & Support message costs, use the live platform pricing section or get_pricing_catalog — never hardcode dollar amounts.

                %s%s%sLDMS reference documents:
                ---
                %s
                ---
                """.formatted(mode.getLabel(), modeSection, LexiBotPersonality.personalityInstructions(mode),
                pricingSection.isBlank() ? "" : pricingSection + "\n\n",
                callerContext, docSection, faqSection, knowledgeText);
    }

    private static String assistantModeInstructions() {
        return """
                As Lexi in Assistant mode, answer how LDMS works for end users: onboarding, purchase orders, shipments, trips, billing,
                fleet, help & support, and platform roles (suppliers, customers, transporters, drivers, admins).
                You have read-only tools: search LDMS knowledge, load session context, wallet summary, portal navigation, pricing,
                list support tickets, list user groups, and list organisation users. Use them when the user asks about live data or where to go.
                To create user groups, add users to groups, or open tickets, tell the user to switch to **Agent** mode.""";
    }

    private static String agentModeInstructions() {
        return """
                You are Lexi in **Agent mode** — an autonomous LDMS operator with tool access.
                Use tools to read platform data and perform actions on the user's behalf within their organisation workspace.
                When the user asks you to do something:
                1. Call get_session_context if you need organisation classification or scope.
                2. Call search_system_knowledge for service ownership, RabbitMQ events, and workflow steps.
                3. Call list_user_groups / list_org_users to resolve IDs before add_users_to_user_group.
                4. Call create_user_group when the user wants a new user group (name required; description optional).
                5. Call add_users_to_user_group with userGroupId and userIds from list_org_users.
                6. Call get_portal_navigation to deep-link the user to the correct portal screen.
                7. Call get_wallet_summary before actions that consume wallet balance.
                8. Call create_support_ticket when human ops must intervene or a workflow cannot be completed in chat.
                After tool calls, summarise what you did and give clear next steps (portal path, group name, or ticket number).
                Never fabricate API endpoints, events, or data — use tool results and reference docs only.
                Prefer technical accuracy and actionable outcomes over generic advice.
                CRITICAL: Invoke tools only through the native tool API. Never write <tool_call>, <tool_response>, JSON tool traces,
                or "Tool calls made:" lists in your reply — users must never see internal tool syntax.""";
    }

    /**
     * Keyword search over bundled LDMS reference markdown (for agent tool use).
     */
    public String searchReferenceKnowledge(String query) {
        if (query == null || query.isBlank() || knowledgeText.isBlank()) {
            return "";
        }
        String[] terms = query.toLowerCase(Locale.ROOT).split("\\s+");
        String[] paragraphs = knowledgeText.split("\\n\\n+");
        StringBuilder hits = new StringBuilder();
        int count = 0;
        for (String paragraph : paragraphs) {
            String lower = paragraph.toLowerCase(Locale.ROOT);
            boolean match = false;
            for (String term : terms) {
                if (term.length() > 2 && lower.contains(term)) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                continue;
            }
            if (count > 0) {
                hits.append("\n\n---\n\n");
            }
            hits.append(paragraph.trim());
            count++;
            if (count >= 5) {
                break;
            }
        }
        return hits.toString().trim();
    }

    public String systemPrompt(String userDisplayName, String organizationName) {
        return systemPrompt(userDisplayName, organizationName, null, null, null);
    }

    private String buildCallerContext(String userDisplayName, String organizationName) {
        boolean hasUser = userDisplayName != null && !userDisplayName.isBlank();
        boolean hasOrg = organizationName != null && !organizationName.isBlank();
        if (!hasUser && !hasOrg) {
            return "";
        }
        StringBuilder block = new StringBuilder("Current user context:\n");
        if (hasUser) {
            block.append("- User: ").append(userDisplayName.trim()).append('\n');
        }
        if (hasOrg) {
            block.append("- Organisation: ").append(organizationName.trim()).append('\n');
        }
        block.append('\n');
        return block.toString();
    }

    private void appendConfiguredResources(Set<String> sources, StringBuilder merged) {
        List<String> configured = new ArrayList<>(properties.getResources());
        if (configured.isEmpty() && properties.getResource() != null && !properties.getResource().isBlank()) {
            configured.add(properties.getResource().trim());
        }
        for (String location : configured) {
            appendResource(location, sources, merged);
        }
    }

    private void appendClasspathKnowledgeDir(Set<String> sources, StringBuilder merged) {
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:ldms-knowledge/*.md");
            List<Resource> sorted = Stream.of(resources)
                    .filter(Resource::exists)
                    .sorted(Comparator.comparing(r -> safeFilename(r.getFilename())))
                    .toList();
            for (Resource resource : sorted) {
                String label = "classpath:ldms-knowledge/" + safeFilename(resource.getFilename());
                if (sources.contains(label)) {
                    continue;
                }
                appendResourceBody(label, resource, sources, merged);
            }
        } catch (IOException ex) {
            log.warn("Could not scan classpath:ldms-knowledge/*.md: {}", ex.getMessage());
        }
    }

    private void appendExternalDirectory(Set<String> sources, StringBuilder merged) {
        String directory = properties.getDirectory();
        if (directory == null || directory.isBlank()) {
            return;
        }
        try {
            Path root = Path.of(directory.replaceFirst("^file:", ""));
            if (!Files.isDirectory(root)) {
                log.warn("Bot knowledge directory not found: {}", root);
                return;
            }
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                        .sorted()
                        .forEach(path -> appendFile(path, sources, merged));
            }
        } catch (Exception ex) {
            log.warn("Failed to load external bot knowledge from {}: {}", directory, ex.getMessage());
        }
    }

    private void appendResource(String location, Set<String> sources, StringBuilder merged) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                log.warn("LDMS knowledge resource not found: {}", location);
                return;
            }
            appendResourceBody(location, resource, sources, merged);
        } catch (IOException ex) {
            log.error("Failed to load LDMS knowledge from {}", location, ex);
        }
    }

    private void appendResourceBody(String label, Resource resource, Set<String> sources, StringBuilder merged)
            throws IOException {
        if (sources.contains(label)) {
            return;
        }
        String text = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8).trim();
        if (text.isBlank()) {
            return;
        }
        sources.add(label);
        if (!merged.isEmpty()) {
            merged.append("\n\n### ").append(label).append("\n\n");
        }
        merged.append(text);
    }

    private void appendFile(Path path, Set<String> sources, StringBuilder merged) {
        String label = path.toAbsolutePath().normalize().toString();
        if (sources.contains(label)) {
            return;
        }
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8).trim();
            if (text.isBlank()) {
                return;
            }
            sources.add(label);
            if (!merged.isEmpty()) {
                merged.append("\n\n### ").append(path.getFileName()).append("\n\n");
            }
            merged.append(text);
        } catch (IOException ex) {
            log.warn("Failed to read knowledge file {}: {}", path, ex.getMessage());
        }
    }

    private static String safeFilename(String filename) {
        return filename == null ? "document.md" : filename;
    }
}
