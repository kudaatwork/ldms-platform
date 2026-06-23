package projectlx.billing.payments.business.logic.support;

import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.billing.payments.model.SubscriptionPackage;
import projectlx.billing.payments.repository.UsageChargeRecordRepository;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Monthly SMS / WhatsApp quota for subscription packages ({@code included_standard_credits}).
 */
public final class SubscriptionMessagingQuotaSupport {

    public static final String ACTION_NOTIFICATION_SMS = "NOTIFICATION_SMS";
    public static final String ACTION_WHATSAPP_COMMAND = "WHATSAPP_COMMAND";

    private SubscriptionMessagingQuotaSupport() {
    }

    public static boolean isMessagingAction(String actionCode) {
        if (actionCode == null) {
            return false;
        }
        String normalized = actionCode.trim().toUpperCase();
        return ACTION_NOTIFICATION_SMS.equals(normalized) || ACTION_WHATSAPP_COMMAND.equals(normalized);
    }

    public static int resolveSmsQuota(SubscriptionPackage pkg) {
        if (pkg == null || pkg.getIncludedStandardCredits() == null) {
            return 0;
        }
        return Math.max(0, pkg.getIncludedStandardCredits());
    }

    public static LocalDateTime periodStart(OrganizationBillingSetting setting) {
        if (setting != null && setting.getSubscriptionStartedAt() != null) {
            LocalDateTime started = setting.getSubscriptionStartedAt();
            LocalDate anchor = started.toLocalDate();
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
            if (anchor.isAfter(monthStart)) {
                return anchor.atStartOfDay();
            }
        }
        return LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
    }

    public static LocalDateTime periodEnd(OrganizationBillingSetting setting) {
        if (setting != null && setting.getSubscriptionRenewsAt() != null) {
            return setting.getSubscriptionRenewsAt();
        }
        return LocalDate.now().plusMonths(1).with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
    }

    public static long countMessagingUsed(
            Long organizationId,
            OrganizationBillingSetting setting,
            UsageChargeRecordRepository usageChargeRecordRepository) {
        if (organizationId == null || organizationId < 1L) {
            return 0L;
        }
        return usageChargeRecordRepository.countMessagingUsageInPeriod(
                organizationId,
                periodStart(setting),
                periodEnd(setting),
                EntityStatus.DELETED);
    }

    public static int smsRemaining(int quota, long used) {
        return Math.max(0, quota - (int) Math.min(Integer.MAX_VALUE, used));
    }

    public static boolean isQuotaExhausted(int quota, long used) {
        return quota > 0 && used >= quota;
    }
}
