package projectlx.billing.payments.business.logic.support;

import projectlx.billing.payments.model.OrganizationBillingSetting;
import projectlx.billing.payments.model.SubscriptionPackage;
import projectlx.billing.payments.repository.OrganizationBillingSettingRepository;
import projectlx.billing.payments.repository.SubscriptionPackageRepository;
import projectlx.billing.payments.utils.enums.OrganizationBillingMode;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

/**
 * Resolves whether an organisation may opt in to fuel consumption based on billing mode and package.
 */
public final class OrganizationFuelConsumptionAvailabilitySupport {

    private OrganizationFuelConsumptionAvailabilitySupport() {
    }

    public static boolean isAvailableForOrganization(
            Long organizationId,
            OrganizationBillingSettingRepository organizationBillingSettingRepository,
            SubscriptionPackageRepository subscriptionPackageRepository) {
        if (organizationId == null || organizationId < 1L) {
            return false;
        }
        OrganizationBillingSetting setting = organizationBillingSettingRepository
                .findByOrganizationIdAndEntityStatusNot(organizationId, EntityStatus.DELETED)
                .orElse(null);
        if (setting == null || setting.getBillingMode() != OrganizationBillingMode.PREMIUM_SUBSCRIPTION) {
            return true;
        }
        Long packageId = setting.getSubscriptionPackageId();
        if (packageId == null || packageId < 1L) {
            return true;
        }
        SubscriptionPackage pkg = subscriptionPackageRepository
                .findByIdAndEntityStatusNot(packageId, EntityStatus.DELETED)
                .orElse(null);
        if (pkg == null) {
            return true;
        }
        return Boolean.TRUE.equals(pkg.getFuelConsumptionAvailable());
    }
}
