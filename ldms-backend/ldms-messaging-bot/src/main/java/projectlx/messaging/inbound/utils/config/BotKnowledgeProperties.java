package projectlx.messaging.inbound.utils.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "ldms.bot.knowledge")
public class BotKnowledgeProperties {

    /** Legacy single-document path (used when {@link #resources} is empty). */
    private String resource = "classpath:ldms-knowledge/ldms-system-architecture.md";

    /** Additional classpath or file resources merged into the assistant context. */
    private List<String> resources = new ArrayList<>();

    /**
     * Optional filesystem directory of {@code *.md} files (e.g. {@code file:/opt/ldms/bot-knowledge}).
     * Updated files are picked up on reload without redeploying the service.
     */
    private String directory = "";

    /** Reload knowledge from disk/classpath every N minutes. 0 = startup only (plus manual reload). */
    private int reloadIntervalMinutes = 0;
}
