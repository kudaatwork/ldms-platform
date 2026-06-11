package projectlx.co.zw.organizationmanagement.business.logic.support;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.utils.config.OrganizationPortalLinkProperties;
import projectlx.co.zw.organizationmanagement.utils.enums.FleetVehicleOwnershipType;
import projectlx.co.zw.organizationmanagement.utils.requests.NotificationRequest;

/**
 * Sends fleet registration notifications to the registering organisation and, when applicable,
 * the contracted transport company.
 */
@Component
@RequiredArgsConstructor
public class OrganizationFleetNotifier {

    private static final Logger log = LoggerFactory.getLogger(OrganizationFleetNotifier.class);

    private static final String EXCHANGE = "notifications.direct";
    private static final String ROUTING_KEY = "notifications.send";
    private static final String TEMPLATE_FLEET_REGISTERED = "ORG_FLEET_REGISTERED";

    private final RabbitTemplate rabbitTemplate;
    private final OrganizationPortalLinkProperties portalLinks;

    public void sendFleetRegistered(
            Organization registeringOrganization,
            Organization transporterOrganization,
            String registration,
            String makeModel,
            String assetType,
            FleetVehicleOwnershipType ownershipType,
            String performedBy) {
        if (registeringOrganization == null || !StringUtils.hasText(registration)) {
            return;
        }

        FleetVehicleOwnershipType resolvedOwnership = ownershipType != null
                ? ownershipType
                : FleetVehicleOwnershipType.OWNED;
        boolean contracted = resolvedOwnership == FleetVehicleOwnershipType.CONTRACTED;
        Long transporterId = transporterOrganization != null ? transporterOrganization.getId() : null;
        boolean notifyTransporterSeparately = contracted
                && transporterOrganization != null
                && transporterId != null
                && !transporterId.equals(registeringOrganization.getId());

        Map<String, Object> fleetFields = fleetFields(
                registeringOrganization,
                transporterOrganization,
                registration,
                makeModel,
                assetType,
                resolvedOwnership);

        Map<String, Object> registeringData = recipientData(registeringOrganization, performedBy, fleetFields);
        registeringData.put(
                "fleetMessageIntro",
                contracted
                        ? "A contracted fleet asset has been registered for your organisation on Project LX LDMS."
                        : "A new fleet asset has been added to your organisation on Project LX LDMS.");
        registeringData.put("fleetPerspective", contracted ? "registering-contracted" : "registering-owned");
        sendToOrganizationMandatoryEmails(registeringOrganization, TEMPLATE_FLEET_REGISTERED, registeringData);

        if (notifyTransporterSeparately) {
            Map<String, Object> transporterData = recipientData(transporterOrganization, performedBy, fleetFields);
            transporterData.put(
                    "fleetMessageIntro",
                    "A fleet asset has been registered with your transport company on Project LX LDMS.");
            transporterData.put("fleetPerspective", "transporter");
            sendToOrganizationMandatoryEmails(transporterOrganization, TEMPLATE_FLEET_REGISTERED, transporterData);
        }
    }

    private Map<String, Object> recipientData(
            Organization recipientOrganization, String performedBy, Map<String, Object> fleetFields) {
        Map<String, Object> data = new LinkedHashMap<>(baseData(recipientOrganization, performedBy));
        data.putAll(fleetFields);
        return data;
    }

    private Map<String, Object> fleetFields(
            Organization registeringOrganization,
            Organization transporterOrganization,
            String registration,
            String makeModel,
            String assetType,
            FleetVehicleOwnershipType ownershipType) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("registration", safe(registration).toUpperCase(Locale.ROOT));
        data.put("makeModel", safe(makeModel));
        data.put("assetType", safe(assetType));
        data.put("assetTypeLabel", formatAssetTypeLabel(assetType));
        data.put("ownershipType", ownershipType.name());
        data.put("ownershipLabel", ownershipType == FleetVehicleOwnershipType.CONTRACTED
                ? "Contracted"
                : "Owned");
        data.put("registeringOrganizationName", safe(registeringOrganization.getName()));
        data.put("transporterName", transporterOrganization != null
                ? safe(transporterOrganization.getName())
                : "—");
        return data;
    }

    private Map<String, Object> baseData(Organization organization, String performedBy) {
        Map<String, Object> data = new LinkedHashMap<>();
        String contactName = String.join(
                        " ",
                        safe(organization.getContactPersonFirstName()),
                        safe(organization.getContactPersonLastName()))
                .trim();
        if (!StringUtils.hasText(contactName)) {
            contactName = safe(organization.getName());
        }
        data.put("organizationName", safe(organization.getName()));
        data.put("contactName", contactName);
        data.put("performedBy", safe(performedBy));
        data.put("signInLink", portalLinks.signInUrlFor(organization));
        return data;
    }

    private void sendToOrganizationMandatoryEmails(
            Organization organization, String templateKey, Map<String, Object> data) {
        sendToOrganizationWithExtraEmail(organization, templateKey, data, null, null, null);
    }

    private void sendToOrganizationWithExtraEmail(
            Organization organization,
            String templateKey,
            Map<String, Object> data,
            String extraEmail,
            String extraPhone,
            String extraRole) {
        if (organization == null || data == null) {
            return;
        }

        Set<String> sentEmails = new LinkedHashSet<>();
        String organizationEmail = OrganizationNotificationEmailSupport.normalizeEmail(organization.getEmail());
        String contactEmail = OrganizationNotificationEmailSupport.normalizeEmail(organization.getContactPersonEmail());
        String normalizedExtraEmail = OrganizationNotificationEmailSupport.normalizeEmail(extraEmail);

        if (StringUtils.hasText(organizationEmail)) {
            sendToEmail(
                    organization.getId(),
                    organizationEmail,
                    safe(organization.getPhoneNumber()),
                    templateKey,
                    data,
                    "organization");
            sentEmails.add(organizationEmail);
        }
        if (StringUtils.hasText(contactEmail) && !sentEmails.contains(contactEmail)) {
            sendToEmail(
                    organization.getId(),
                    contactEmail,
                    safe(organization.getContactPersonPhoneNumber()),
                    templateKey,
                    data,
                    "contact-person");
            sentEmails.add(contactEmail);
        }
        if (StringUtils.hasText(normalizedExtraEmail) && !sentEmails.contains(normalizedExtraEmail)) {
            sendToEmail(
                    organization.getId(),
                    normalizedExtraEmail,
                    safe(extraPhone),
                    templateKey,
                    data,
                    StringUtils.hasText(extraRole) ? extraRole : "extra-recipient");
        }
    }

    private void sendToEmail(
            Long organizationId,
            String email,
            String phoneNumber,
            String templateKey,
            Map<String, Object> baseData,
            String recipientRole) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        String normalizedEmail = OrganizationNotificationEmailSupport.normalizeEmail(email);
        Map<String, Object> data = new LinkedHashMap<>(baseData);
        data.put("email", normalizedEmail);
        data.put("Email", normalizedEmail);
        data.put("recipientRole", recipientRole);
        data.put("phoneNumber", safe(phoneNumber));

        String recipientUserId = organizationId != null
                ? organizationId + ":" + recipientRole
                : recipientRole;
        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID().toString(),
                templateKey,
                new NotificationRequest.Recipient(
                        recipientUserId,
                        normalizedEmail,
                        safe(phoneNumber),
                        null),
                data,
                new NotificationRequest.Metadata("ldms-organization-management", null));
        try {
            log.info(
                    "Publishing fleet registration notification template={} role={} to={}",
                    templateKey,
                    recipientRole,
                    normalizedEmail);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
        } catch (Exception e) {
            log.error(
                    "Failed publishing fleet registration notification template={} role={} to={}: {}",
                    templateKey,
                    recipientRole,
                    normalizedEmail,
                    e.getMessage());
        }
    }

    private static String formatAssetTypeLabel(String assetType) {
        String raw = safe(assetType);
        if (!StringUtils.hasText(raw)) {
            return "Asset";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = normalized.split("\\s+");
        StringBuilder label = new StringBuilder();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                label.append(part.substring(1));
            }
        }
        return !label.isEmpty() ? label.toString() : "Asset";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
