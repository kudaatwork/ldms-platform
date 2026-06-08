package projectlx.co.zw.organizationmanagement.business.logic.support;

/**
 * Fired after a supplier registers a customer or transporter on the platform portal.
 * Handled after transaction commit to send onboarding emails.
 */
public record OrganizationSupplierRegisteredEvent(Long organizationId) {}
