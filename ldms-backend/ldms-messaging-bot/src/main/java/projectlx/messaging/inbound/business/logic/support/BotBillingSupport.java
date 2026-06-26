package projectlx.messaging.inbound.business.logic.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import projectlx.co.zw.shared_library.billing.PlatformWalletActionCodes;
import projectlx.co.zw.shared_library.billing.PlatformWalletUsageSupport;
import projectlx.messaging.inbound.model.BotSession;
import projectlx.messaging.inbound.utils.enums.BotAssistantMode;

@Slf4j
public class BotBillingSupport {

    private final BotCallerProfileSupport botCallerProfileSupport;
    private final PlatformWalletUsageSupport platformWalletUsageSupport;

    public BotBillingSupport(
            BotCallerProfileSupport botCallerProfileSupport,
            PlatformWalletUsageSupport platformWalletUsageSupport) {
        this.botCallerProfileSupport = botCallerProfileSupport;
        this.platformWalletUsageSupport = platformWalletUsageSupport;
    }

    /**
     * Deducts prepaid wallet balance for each user message sent to the LDMS assistant.
     * Organisation context is resolved from the session or the caller profile at charge time.
     */
    public void chargeForUserMessage(BotSession session, String username, Long messageId) {
        Long organizationId = attachOrganizationContext(session, username);
        if (organizationId == null) {
            log.warn("Skipping bot message charge — no organisation context for user {}", username);
            return;
        }
        BotAssistantMode mode = session.getAssistantMode() != null ? session.getAssistantMode() : BotAssistantMode.ASSISTANT;
        String actionCode = mode == BotAssistantMode.AGENT
                ? PlatformWalletActionCodes.HELP_BOT_AGENT_MESSAGE
                : PlatformWalletActionCodes.HELP_BOT_MESSAGE;
        platformWalletUsageSupport.chargeBestEffort(
                organizationId,
                actionCode,
                "BOT_MESSAGE",
                messageId);
    }

    /**
     * Deducts prepaid wallet balance when a user opens a new LDMS Assistant session.
     */
    public void chargeForSessionStart(BotSession session, String username) {
        Long organizationId = attachOrganizationContext(session, username);
        if (organizationId == null) {
            log.warn("Skipping BOT_SESSION_START charge — no organisation context for user {}", username);
            return;
        }
        platformWalletUsageSupport.chargeBestEffort(
                organizationId,
                PlatformWalletActionCodes.BOT_SESSION_START,
                "BOT_SESSION",
                session.getId());
    }

    private Long attachOrganizationContext(BotSession session, String username) {
        if (session.getOrganizationId() != null && session.getOrganizationId() > 0L) {
            return session.getOrganizationId();
        }
        BotCallerProfileSupport.CallerProfile profile = botCallerProfileSupport.resolve(username);
        if (profile.organizationId() == null || profile.organizationId() <= 0L) {
            return null;
        }
        session.setOrganizationId(profile.organizationId());
        if (StringUtils.hasText(profile.organizationName()) && !"—".equals(profile.organizationName())) {
            session.setOrganizationName(profile.organizationName());
        }
        return profile.organizationId();
    }
}
