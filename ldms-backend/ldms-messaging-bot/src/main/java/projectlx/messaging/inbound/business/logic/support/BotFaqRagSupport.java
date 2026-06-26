package projectlx.messaging.inbound.business.logic.support;

import lombok.extern.slf4j.Slf4j;
import projectlx.messaging.inbound.model.BotFaq;
import projectlx.messaging.inbound.repository.BotFaqRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lightweight keyword retrieval over admin-managed FAQ entries (RAG without vector store).
 */
@Slf4j
public class BotFaqRagSupport {

    private static final int DEFAULT_TOP_K = 5;

    private final BotFaqRepository botFaqRepository;

    private volatile List<BotFaq> publishedCache = List.of();

    public BotFaqRagSupport(BotFaqRepository botFaqRepository) {
        this.botFaqRepository = botFaqRepository;
        reload();
    }

    public synchronized void reload() {
        publishedCache = botFaqRepository
                .findByPublishedTrueAndEntityStatusNotOrderByModifiedAtDescCreatedAtDesc(EntityStatus.DELETED);
        log.info("Bot FAQ RAG cache loaded: {} published entries", publishedCache.size());
    }

    public int publishedCount() {
        return publishedCache.size();
    }

    /**
     * Returns formatted FAQ block for the system prompt and records usage on matched entries.
     */
    public String retrieveContextForQuery(String userQuery) {
        List<ScoredFaq> matches = rank(userQuery, DEFAULT_TOP_K);
        if (matches.isEmpty()) {
            return "";
        }
        List<Long> matchedIds = matches.stream().map(s -> s.faq().getId()).toList();
        try {
            botFaqRepository.incrementUseCount(matchedIds);
        } catch (Exception ex) {
            log.warn("Could not increment FAQ use counts: {}", ex.getMessage());
        }

        StringBuilder block = new StringBuilder("""
                Admin-maintained FAQ knowledge (most relevant to the user's question):
                """);
        int index = 1;
        for (ScoredFaq scored : matches) {
            BotFaq faq = scored.faq();
            block.append("\n").append(index++).append(". Q: ").append(faq.getQuestion().trim())
                    .append("\n   A: ").append(faq.getAnswer().trim());
            if (faq.getCategory() != null) {
                block.append("\n   Category: ").append(faq.getCategory().name());
            }
            block.append('\n');
        }
        block.append("""
                Prefer these FAQ answers when they directly address the user's question.
                """);
        return block.toString().trim();
    }

  /**
     * Best matching FAQ answer for guide-mode replies (no LLM).
     */
    public String bestAnswerForQuery(String userQuery) {
        List<ScoredFaq> matches = rank(userQuery, 1);
        if (matches.isEmpty() || matches.get(0).score() < 2) {
            return "";
        }
        return matches.get(0).faq().getAnswer().trim();
    }

    List<ScoredFaq> rank(String userQuery, int topK) {
        if (userQuery == null || userQuery.isBlank() || publishedCache.isEmpty()) {
            return List.of();
        }
        Set<String> queryTokens = tokenize(userQuery);
        if (queryTokens.isEmpty()) {
            return List.of();
        }
        List<ScoredFaq> scored = new ArrayList<>();
        for (BotFaq faq : publishedCache) {
            int score = scoreFaq(faq, queryTokens);
            if (score > 0) {
                scored.add(new ScoredFaq(faq, score));
            }
        }
        scored.sort(Comparator.comparingInt(ScoredFaq::score).reversed()
                .thenComparing(s -> s.faq().getUseCount(), Comparator.reverseOrder()));
        return scored.stream().limit(topK).toList();
    }

    private static int scoreFaq(BotFaq faq, Set<String> queryTokens) {
        String corpus = String.join(" ",
                safe(faq.getQuestion()),
                safe(faq.getAnswer()),
                safe(faq.getKeywords()),
                faq.getCategory() != null ? faq.getCategory().name() : "");
        Set<String> faqTokens = tokenize(corpus);
        int overlap = 0;
        for (String token : queryTokens) {
            if (faqTokens.contains(token)) {
                overlap++;
            }
        }
        String lowerQuery = corpus.toLowerCase(Locale.ROOT);
        for (String token : queryTokens) {
            if (token.length() >= 4 && lowerQuery.contains(token)) {
                overlap += 2;
            }
        }
        return overlap;
    }

    private static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(t -> t.length() >= 3)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    record ScoredFaq(BotFaq faq, int score) {
    }
}
