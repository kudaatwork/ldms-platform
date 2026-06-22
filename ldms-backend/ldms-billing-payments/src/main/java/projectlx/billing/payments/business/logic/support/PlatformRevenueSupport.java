package projectlx.billing.payments.business.logic.support;

import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.billing.payments.model.PlatformActionCharge;
import projectlx.billing.payments.model.PlatformWallet;
import projectlx.billing.payments.model.UsageChargeRecord;
import projectlx.billing.payments.repository.OrganizationBillingSettingRepository;
import projectlx.billing.payments.repository.PlatformActionChargeRepository;
import projectlx.billing.payments.repository.PlatformWalletRepository;
import projectlx.billing.payments.repository.UsageChargeRecordRepository;
import projectlx.billing.payments.repository.WalletDepositRepository;
import projectlx.billing.payments.utils.dtos.PlatformRevenueCategoryRowDto;
import projectlx.billing.payments.utils.dtos.PlatformRevenueChargeLineDto;
import projectlx.billing.payments.utils.dtos.PlatformRevenueOrgRowDto;
import projectlx.billing.payments.utils.dtos.PlatformRevenueReportDto;
import projectlx.billing.payments.utils.dtos.UsageChargeBreakdownDto;
import projectlx.billing.payments.utils.enums.OrganizationBillingMode;
import projectlx.billing.payments.utils.enums.PlatformActionCategory;
import projectlx.billing.payments.utils.enums.WalletDepositStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PlatformRevenueSupport {

    private static final int MONTH_WINDOW = 6;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String[] ACCENTS = {
            "#6366f1", "#0ea5e9", "#10b981", "#f59e0b", "#ec4899", "#8b5cf6", "#14b8a6", "#f97316"
    };

    private static final Map<PlatformActionCategory, String> CATEGORY_COLORS = Map.ofEntries(
            Map.entry(PlatformActionCategory.GENERAL, "#6366f1"),
            Map.entry(PlatformActionCategory.NOTIFICATIONS, "#0ea5e9"),
            Map.entry(PlatformActionCategory.TRIPS, "#10b981"),
            Map.entry(PlatformActionCategory.DOCUMENTS, "#818cf8"),
            Map.entry(PlatformActionCategory.BILLING, "#f59e0b"),
            Map.entry(PlatformActionCategory.PLATFORM, "#ec4899"),
            Map.entry(PlatformActionCategory.IOT, "#14b8a6"),
            Map.entry(PlatformActionCategory.ORDERS, "#f97316"),
            Map.entry(PlatformActionCategory.LOGISTICS, "#22c55e"),
            Map.entry(PlatformActionCategory.FLEET, "#06b6d4"),
            Map.entry(PlatformActionCategory.PROCUREMENT, "#a855f7"),
            Map.entry(PlatformActionCategory.SUPPORT, "#f43f5e"));

    private final UsageChargeRecordRepository usageChargeRecordRepository;
    private final WalletDepositRepository walletDepositRepository;
    private final OrganizationBillingSettingRepository organizationBillingSettingRepository;
    private final PlatformWalletRepository platformWalletRepository;
    private final PlatformActionChargeRepository platformActionChargeRepository;
    private final OrganizationNameResolver organizationNameResolver;

    public PlatformRevenueSupport(
            UsageChargeRecordRepository usageChargeRecordRepository,
            WalletDepositRepository walletDepositRepository,
            OrganizationBillingSettingRepository organizationBillingSettingRepository,
            PlatformWalletRepository platformWalletRepository,
            PlatformActionChargeRepository platformActionChargeRepository,
            OrganizationNameResolver organizationNameResolver) {
        this.usageChargeRecordRepository = usageChargeRecordRepository;
        this.walletDepositRepository = walletDepositRepository;
        this.organizationBillingSettingRepository = organizationBillingSettingRepository;
        this.platformWalletRepository = platformWalletRepository;
        this.platformActionChargeRepository = platformActionChargeRepository;
        this.organizationNameResolver = organizationNameResolver;
    }

    public PlatformRevenueReportDto buildRevenueReport() {
        EntityStatus deleted = EntityStatus.DELETED;
        OrganizationBillingMode subscriptionMode = OrganizationBillingMode.PREMIUM_SUBSCRIPTION;
        Map<String, PlatformActionCategory> categoryByAction = loadCategoryByAction(deleted);

        Map<Long, OrganizationBillingSetting> settingsByOrg = new HashMap<>();
        for (OrganizationBillingSetting setting : organizationBillingSettingRepository.findByEntityStatusNot(deleted)) {
            settingsByOrg.put(setting.getOrganizationId(), setting);
        }

        Map<Long, Long> walletBalanceByOrg = new HashMap<>();
        for (PlatformWallet wallet : platformWalletRepository.findByEntityStatusNot(deleted)) {
            walletBalanceByOrg.put(wallet.getOrganizationId(), wallet.getBalanceCents() != null ? wallet.getBalanceCents() : 0L);
        }

        Map<Long, OrgUsageTotals> usageByOrg = new HashMap<>();
        for (Object[] row : usageChargeRecordRepository.aggregateUsageByOrganization(subscriptionMode, deleted)) {
            Long orgId = (Long) row[0];
            long actionChargeCents = toLong(row[1]);
            long subscriptionUsageCents = toLong(row[2]);
            long totalUsageCents = toLong(row[3]);
            long usageEventCount = toLong(row[4]);
            usageByOrg.put(orgId, new OrgUsageTotals(actionChargeCents, subscriptionUsageCents, totalUsageCents, usageEventCount));
        }

        Map<Long, Long> depositsByOrg = new HashMap<>();
        for (Object[] row : walletDepositRepository.aggregateDepositsByOrganization(WalletDepositStatus.CONFIRMED, deleted)) {
            depositsByOrg.put((Long) row[0], toLong(row[1]));
        }

        Set<Long> orgIds = new TreeSet<>();
        orgIds.addAll(usageByOrg.keySet());
        orgIds.addAll(depositsByOrg.keySet());
        orgIds.addAll(settingsByOrg.keySet());
        orgIds.addAll(walletBalanceByOrg.keySet());

        List<PlatformRevenueOrgRowDto> orgRows = new ArrayList<>();
        long totalActionCharges = 0L;
        long totalSubscriptionUsage = 0L;
        long totalDeposits = 0L;
        int accentIndex = 0;

        for (Long orgId : orgIds) {
            OrgUsageTotals usage = usageByOrg.getOrDefault(orgId, OrgUsageTotals.EMPTY);
            long depositCents = depositsByOrg.getOrDefault(orgId, 0L);
            if (usage.totalUsageCents() <= 0 && depositCents <= 0) {
                continue;
            }

            OrganizationBillingSetting setting = settingsByOrg.get(orgId);
            String orgName = resolveOrgName(orgId, setting);
            String billingMode = setting != null && setting.getBillingMode() != null
                    ? setting.getBillingMode().name()
                    : OrganizationBillingMode.PREPAID_WALLET.name();

            long earnedCents = depositCents + usage.actionChargeCents();
            long costsCents = usage.subscriptionUsageCents();

            PlatformRevenueOrgRowDto orgRow = new PlatformRevenueOrgRowDto();
            orgRow.setOrganizationId(orgId);
            orgRow.setOrganizationName(orgName);
            orgRow.setBillingMode(billingMode);
            orgRow.setEarnedCents(earnedCents);
            orgRow.setCostsCents(costsCents);
            orgRow.setNetCents(earnedCents - costsCents);
            orgRow.setWalletBalanceCents(walletBalanceByOrg.getOrDefault(orgId, 0L));
            orgRow.setDepositCents(depositCents);
            orgRow.setActionChargeCents(usage.actionChargeCents());
            orgRow.setSubscriptionUsageCents(usage.subscriptionUsageCents());
            orgRow.setTotalUsageCents(usage.totalUsageCents());
            orgRow.setUsageEventCount(usage.usageEventCount());
            orgRow.setAccent(ACCENTS[accentIndex++ % ACCENTS.length]);
            orgRow.setUsageBreakdown(buildUsageBreakdown(orgId, deleted));
            orgRows.add(orgRow);

            totalActionCharges += usage.actionChargeCents();
            totalSubscriptionUsage += usage.subscriptionUsageCents();
            totalDeposits += depositCents;
        }

        orgRows.sort(Comparator.comparingLong(PlatformRevenueOrgRowDto::getEarnedCents).reversed()
                .thenComparing(PlatformRevenueOrgRowDto::getTotalUsageCents, Comparator.reverseOrder()));

        LocalDateTime seriesFrom = LocalDate.now().minusMonths(MONTH_WINDOW - 1L).withDayOfMonth(1).atStartOfDay();
        Map<YearMonthKey, Long> earnedByMonth = new HashMap<>();
        mergeMonthlyTotals(earnedByMonth, usageChargeRecordRepository.sumDeductedChargesByMonth(seriesFrom, deleted));
        mergeMonthlyTotals(earnedByMonth, walletDepositRepository.sumConfirmedDepositsByMonth(
                seriesFrom, WalletDepositStatus.CONFIRMED, deleted));

        Map<YearMonthKey, Long> subscriptionByMonth = new HashMap<>();
        mergeMonthlyTotals(subscriptionByMonth, usageChargeRecordRepository.sumSubscriptionUsageByMonth(
                seriesFrom, subscriptionMode, deleted));

        List<String> monthLabels = new ArrayList<>();
        List<Long> earnedSeries = new ArrayList<>();
        List<Long> costSeries = new ArrayList<>();
        LocalDate cursor = seriesFrom.toLocalDate();
        LocalDate endMonth = LocalDate.now().withDayOfMonth(1);
        while (!cursor.isAfter(endMonth)) {
            YearMonthKey key = YearMonthKey.of(cursor.getYear(), cursor.getMonthValue());
            monthLabels.add(cursor.getMonth().name().substring(0, 1)
                    + cursor.getMonth().name().substring(1).toLowerCase(Locale.ROOT).substring(0, 2));
            earnedSeries.add(earnedByMonth.getOrDefault(key, 0L));
            costSeries.add(subscriptionByMonth.getOrDefault(key, 0L));
            cursor = cursor.plusMonths(1);
        }

        PlatformRevenueReportDto report = new PlatformRevenueReportDto();
        report.setTotalEarnedCents(totalDeposits + totalActionCharges);
        report.setSubscriptionCents(totalSubscriptionUsage);
        report.setActionChargesCents(totalActionCharges);
        report.setWalletDepositsCents(totalDeposits);
        report.setMonthLabels(monthLabels);
        report.setEarnedSeries(earnedSeries);
        report.setCostSeries(costSeries);
        report.setByOrganization(orgRows);
        report.setCostBreakdown(buildCategoryBreakdown(deleted, categoryByAction));
        report.setRecentCharges(buildRecentCharges(deleted, settingsByOrg, categoryByAction));
        return report;
    }

    private List<UsageChargeBreakdownDto> buildUsageBreakdown(Long organizationId, EntityStatus deleted) {
        List<UsageChargeBreakdownDto> rows = new ArrayList<>();
        for (Object[] row : usageChargeRecordRepository.sumByActionForOrganization(organizationId, deleted)) {
            UsageChargeBreakdownDto dto = new UsageChargeBreakdownDto();
            dto.setActionCode((String) row[0]);
            dto.setActionDisplayName((String) row[1]);
            dto.setTotalChargeCents(toLong(row[2]));
            dto.setEventCount(toLong(row[3]));
            rows.add(dto);
        }
        return rows;
    }

    private List<PlatformRevenueCategoryRowDto> buildCategoryBreakdown(
            EntityStatus deleted,
            Map<String, PlatformActionCategory> categoryByAction) {
        Map<PlatformActionCategory, Long> totals = new LinkedHashMap<>();
        for (Object[] row : usageChargeRecordRepository.sumChargesByActionCode(deleted)) {
            String actionCode = (String) row[0];
            long cents = toLong(row[1]);
            PlatformActionCategory category = categoryByAction.getOrDefault(actionCode, PlatformActionCategory.GENERAL);
            totals.merge(category, cents, Long::sum);
        }

        return totals.entrySet().stream()
                .sorted(Map.Entry.<PlatformActionCategory, Long>comparingByValue().reversed())
                .map(entry -> {
                    PlatformRevenueCategoryRowDto row = new PlatformRevenueCategoryRowDto();
                    row.setCategory(formatCategoryLabel(entry.getKey()));
                    row.setAmountCents(entry.getValue());
                    row.setColor(CATEGORY_COLORS.getOrDefault(entry.getKey(), "#6366f1"));
                    return row;
                })
                .toList();
    }

    private List<PlatformRevenueChargeLineDto> buildRecentCharges(
            EntityStatus deleted,
            Map<Long, OrganizationBillingSetting> settingsByOrg,
            Map<String, PlatformActionCategory> categoryByAction) {
        List<PlatformRevenueChargeLineDto> lines = new ArrayList<>();
        for (UsageChargeRecord record : usageChargeRecordRepository.findTop100ByEntityStatusNotOrderByCreatedAtDesc(deleted)) {
            PlatformRevenueChargeLineDto line = new PlatformRevenueChargeLineDto();
            line.setId(String.valueOf(record.getId()));
            line.setLabel(record.getActionDisplayName() != null ? record.getActionDisplayName() : record.getActionCode());
            PlatformActionCategory category = categoryByAction.getOrDefault(
                    record.getActionCode(), PlatformActionCategory.GENERAL);
            line.setCategory(formatCategoryLabel(category));
            line.setAmountCents(record.getChargeCents() != null ? record.getChargeCents() : 0L);
            line.setOrganizationId(record.getOrganizationId());
            line.setOrganizationName(resolveOrgName(record.getOrganizationId(), settingsByOrg.get(record.getOrganizationId())));
            line.setOccurredAt(record.getCreatedAt() != null ? record.getCreatedAt().format(ISO) : null);
            line.setDeducted(Boolean.TRUE.equals(record.getDeducted()));
            lines.add(line);
        }
        return lines;
    }

    private Map<String, PlatformActionCategory> loadCategoryByAction(EntityStatus deleted) {
        Map<String, PlatformActionCategory> map = new HashMap<>();
        for (PlatformActionCharge charge : platformActionChargeRepository.findByEntityStatusNotOrderByCategoryAscDisplayNameAsc(deleted)) {
            map.put(charge.getActionCode(), charge.getCategory() != null ? charge.getCategory() : PlatformActionCategory.GENERAL);
        }
        return map;
    }

    private void mergeMonthlyTotals(Map<YearMonthKey, Long> target, List<Object[]> rows) {
        for (Object[] row : rows) {
            YearMonthKey key = YearMonthKey.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            target.merge(key, toLong(row[2]), Long::sum);
        }
    }

    private String resolveOrgName(Long organizationId, OrganizationBillingSetting setting) {
        String stored = setting != null ? setting.getOrganizationName() : null;
        return organizationNameResolver.resolve(organizationId, stored);
    }

    private String formatCategoryLabel(PlatformActionCategory category) {
        return switch (category) {
            case NOTIFICATIONS -> "SMS & notifications";
            case TRIPS -> "Trip telemetry";
            case DOCUMENTS -> "Document storage";
            case BILLING -> "Payment processing";
            case PLATFORM -> "Platform services";
            case IOT -> "IoT & devices";
            case ORDERS -> "Orders & POs";
            case LOGISTICS -> "Logistics actions";
            case FLEET -> "Fleet operations";
            case PROCUREMENT -> "Procurement";
            case SUPPORT -> "Help & support";
            default -> "General platform";
        };
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private record OrgUsageTotals(
            long actionChargeCents,
            long subscriptionUsageCents,
            long totalUsageCents,
            long usageEventCount) {

        private static final OrgUsageTotals EMPTY = new OrgUsageTotals(0L, 0L, 0L, 0L);
    }

    private record YearMonthKey(int year, int month) {

        static YearMonthKey of(int year, int month) {
            return new YearMonthKey(year, month);
        }
    }
}
