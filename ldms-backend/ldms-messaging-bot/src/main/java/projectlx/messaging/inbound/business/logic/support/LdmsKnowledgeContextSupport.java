package projectlx.messaging.inbound.business.logic.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StreamUtils;
import projectlx.messaging.inbound.utils.config.BotKnowledgeProperties;
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

    private volatile String knowledgeText = "";
    private volatile LocalDateTime lastLoadedAt;
    private volatile List<String> loadedSources = List.of();

    public LdmsKnowledgeContextSupport(ResourcePatternResolver resourcePatternResolver,
                                       ResourceLoader resourceLoader,
                                       BotKnowledgeProperties properties) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.resourceLoader = resourceLoader;
        this.properties = properties;
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

    public BotKnowledgeStatusDto status() {
        BotKnowledgeStatusDto dto = new BotKnowledgeStatusDto();
        dto.setLastLoadedAt(lastLoadedAt);
        dto.setDocumentCount(loadedSources.size());
        dto.setCharacterCount(knowledgeText.length());
        dto.setSources(loadedSources);
        return dto;
    }

    public String systemPrompt(String userDisplayName, String organizationName) {
        String callerContext = buildCallerContext(userDisplayName, organizationName);
        return """
                You are the LDMS (Logistics and Distribution Management System) assistant for Project LX.
                Answer questions about how LDMS works: onboarding, purchase orders, shipments, trips, billing,
                fleet, help & support, and platform roles (suppliers, customers, transporters, drivers, admins).

                Rules:
                - Use ONLY the LDMS reference documents below and the conversation history. Do not invent features.
                - If the answer is not in the documents, say you are not sure and suggest opening a Help & Support ticket.
                - Be concise, practical, and friendly. Use bullet points for multi-step flows.
                - Never reveal API keys, internal credentials, or JWT secrets.
                - For live shipment/trip/invoice status, explain where to find it in the portal (e.g. Track shipments, Billing).
                - When the user belongs to an organisation, tailor examples to their role where the documents allow it.

                %s
                LDMS reference documents:
                ---
                %s
                ---
                """.formatted(callerContext, knowledgeText);
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
