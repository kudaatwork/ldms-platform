package projectlx.messaging.inbound.business.logic.support;

import lombok.extern.slf4j.Slf4j;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.messaging.inbound.model.BotKnowledgeDocument;
import projectlx.messaging.inbound.repository.BotKnowledgeDocumentRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lightweight keyword retrieval over admin-uploaded PDF knowledge documents (RAG without vector store).
 * Splits each document's extracted_text into ~1500-character chunks and ranks the top-K by keyword overlap.
 */
@Slf4j
public class BotKnowledgeDocumentRagSupport {

    private static final int CHUNK_SIZE = 1500;
    private static final int DEFAULT_TOP_K = 5;

    private final BotKnowledgeDocumentRepository botKnowledgeDocumentRepository;

    private volatile List<DocumentChunk> chunkCache = List.of();
    private volatile int publishedDocumentCount = 0;
    private volatile int totalCharacterCount = 0;

    public BotKnowledgeDocumentRagSupport(BotKnowledgeDocumentRepository botKnowledgeDocumentRepository) {
        this.botKnowledgeDocumentRepository = botKnowledgeDocumentRepository;
        reload();
    }

    public synchronized void reload() {
        List<BotKnowledgeDocument> documents = botKnowledgeDocumentRepository
                .findByPublishedTrueAndEntityStatusNotOrderByModifiedAtDescCreatedAtDesc(EntityStatus.DELETED);

        List<DocumentChunk> chunks = new ArrayList<>();
        int charCount = 0;
        for (BotKnowledgeDocument doc : documents) {
            String text = doc.getExtractedText();
            if (text == null || text.isBlank()) {
                continue;
            }
            charCount += text.length();
            List<String> docChunks = splitIntoChunks(text, CHUNK_SIZE);
            for (int i = 0; i < docChunks.size(); i++) {
                chunks.add(new DocumentChunk(doc.getId(), doc.getTitle(), i, docChunks.get(i)));
            }
        }

        this.chunkCache = List.copyOf(chunks);
        this.publishedDocumentCount = documents.size();
        this.totalCharacterCount = charCount;
        log.info("Bot knowledge document RAG cache loaded: {} document(s), {} chunks, {} characters",
                documents.size(), chunks.size(), charCount);
    }

    public int publishedDocumentCount() {
        return publishedDocumentCount;
    }

    public int totalCharacterCount() {
        return totalCharacterCount;
    }

    /**
     * Returns formatted document chunk block for the system prompt and increments use_count on matched docs.
     */
    public String retrieveContextForQuery(String userQuery) {
        List<ScoredChunk> matches = rank(userQuery, DEFAULT_TOP_K);
        if (matches.isEmpty()) {
            return "";
        }

        Set<Long> matchedDocIds = matches.stream().map(s -> s.chunk().documentId()).collect(Collectors.toSet());
        try {
            botKnowledgeDocumentRepository.incrementUseCount(new ArrayList<>(matchedDocIds));
        } catch (Exception ex) {
            log.warn("Could not increment knowledge document use counts: {}", ex.getMessage());
        }

        StringBuilder block = new StringBuilder("""
                Uploaded PDF knowledge (most relevant sections to the user's question):
                """);
        int index = 1;
        for (ScoredChunk scored : matches) {
            DocumentChunk chunk = scored.chunk();
            block.append("\n").append(index++).append(". [").append(chunk.documentTitle()).append("]\n")
                    .append(chunk.text().trim()).append('\n');
        }
        block.append("""
                Prefer these uploaded knowledge sections when they directly address the user's question.
                """);
        return block.toString().trim();
    }

    /**
     * Clean excerpt for guide-mode user replies (no internal RAG scaffolding).
     */
    public String bestSnippetForUserReply(String userQuery) {
        List<ScoredChunk> matches = rank(userQuery, 2);
        if (matches.isEmpty()) {
            return "";
        }

        Set<Long> matchedDocIds = matches.stream().map(s -> s.chunk().documentId()).collect(Collectors.toSet());
        try {
            botKnowledgeDocumentRepository.incrementUseCount(new ArrayList<>(matchedDocIds));
        } catch (Exception ex) {
            log.warn("Could not increment knowledge document use counts: {}", ex.getMessage());
        }

        StringBuilder snippet = new StringBuilder();
        for (ScoredChunk scored : matches) {
            if (snippet.length() > 0) {
                snippet.append("\n\n");
            }
            snippet.append(scored.chunk().text().trim());
        }
        String text = snippet.toString().trim();
        return text.length() <= 1200 ? text : text.substring(0, 1200).trim() + "…";
    }

    List<ScoredChunk> rank(String userQuery, int topK) {
        if (userQuery == null || userQuery.isBlank() || chunkCache.isEmpty()) {
            return List.of();
        }
        Set<String> queryTokens = tokenize(userQuery);
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        List<ScoredChunk> scored = new ArrayList<>();
        for (DocumentChunk chunk : chunkCache) {
            int score = scoreChunk(chunk.text(), queryTokens);
            if (score > 0) {
                scored.add(new ScoredChunk(chunk, score));
            }
        }
        scored.sort(Comparator.comparingInt(ScoredChunk::score).reversed());
        return scored.stream().limit(topK).toList();
    }

    private static int scoreChunk(String text, Set<String> queryTokens) {
        Set<String> chunkTokens = tokenize(text);
        String lowerText = text.toLowerCase(Locale.ROOT);
        int overlap = 0;
        for (String token : queryTokens) {
            if (chunkTokens.contains(token)) {
                overlap++;
            }
            if (token.length() >= 4 && lowerText.contains(token)) {
                overlap += 2;
            }
        }
        return overlap;
    }

    private static List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            // Try to break at a whitespace boundary to avoid cutting mid-word
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            start = end + 1;
        }
        return chunks;
    }

    private static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(t -> t.length() >= 3)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    record DocumentChunk(Long documentId, String documentTitle, int chunkIndex, String text) {
    }

    record ScoredChunk(DocumentChunk chunk, int score) {
    }
}
