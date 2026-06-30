package projectlx.co.zw.organizationmanagement.business.logic.support;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import projectlx.co.zw.organizationmanagement.clients.UserManagementServiceClient;
import projectlx.co.zw.organizationmanagement.model.Agent;
import projectlx.co.zw.organizationmanagement.model.Branch;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.utils.config.OrganizationPortalLinkProperties;
import projectlx.co.zw.organizationmanagement.utils.requests.NotificationRequest;
import projectlx.co.zw.shared_library.utils.dtos.UserDto;
import projectlx.co.zw.shared_library.utils.notifications.PlatformBellNotificationPublisher;
import projectlx.co.zw.shared_library.utils.requests.PlatformBellNotificationRequest;
import projectlx.co.zw.shared_library.utils.responses.UserResponse;

import java.util.List;

/**
 * Sends organisation directory notifications to organisation contacts.
 */
@Component
@RequiredArgsConstructor
public class OrganizationDirectoryNotifier {

    private static final Logger log = LoggerFactory.getLogger(OrganizationDirectoryNotifier.class);

    private static final String EXCHANGE = "notifications.direct";
    private static final String ROUTING_KEY = "notifications.send";

    private static final String TEMPLATE_BRANCH_CREATED = "ORG_BRANCH_CREATED";
    private static final String TEMPLATE_AGENT_CREATED = "ORG_AGENT_CREATED";
    private static final String TEMPLATE_CUSTOMER_LINKED = "ORG_CUSTOMER_LINKED";
    private static final String TEMPLATE_CLEARING_AGENT_LINKED = "ORG_CLEARING_AGENT_LINKED";
    private static final String TEMPLATE_TRANSPORTER_LINKED = "ORG_TRANSPORTER_LINKED";
    private static final String TEMPLATE_TRANSPORTER_OFFER = "ORG_TRANSPORTER_OFFER";

    private static final String SOURCE_SERVICE = "ldms-organization-management";
    private static final String CONNECTION_REQUESTS_ROUTE = "/organization/connection-requests";
    private static final String TRANSPORTERS_ROUTE = "/organization/transporters";
    private static final String EVENT_TRANSPORTER_OFFER = "ORG_TRANSPORTER_OFFER";
    private static final String EVENT_TRANSPORTER_OFFER_ACCEPTED = "ORG_TRANSPORTER_OFFER_ACCEPTED";
    private static final String ENTITY_TRANSPORTER_OFFER = "TRANSPORTER_OFFER";

    private final RabbitTemplate rabbitTemplate;
    private final OrganizationPortalLinkProperties portalLinks;
    private final PlatformBellNotificationPublisher platformBellNotificationPublisher;
    private final UserManagementServiceClient userManagementServiceClient;

    public void sendBranchCreated(Branch branch, String performedBy) {
        if (branch == null || branch.getOrganization() == null) {
            return;
        }
        Organization organization = branch.getOrganization();
        Map<String, Object> data = baseData(organization, performedBy);
        data.put("branchName", safe(branch.getBranchName()));
        data.put("branchCode", safe(branch.getBranchCode()));
        data.put("branchEmail", OrganizationNotificationEmailSupport.normalizeEmail(branch.getEmail()));
        data.put("branchPhoneNumber", safe(branch.getPhoneNumber()));
        data.put("branchRegion", safe(branch.getRegion()));
        data.put("businessHours", safe(branch.getBusinessHours()));
        sendToOrganizationWithExtraEmail(
                organization,
                TEMPLATE_BRANCH_CREATED,
                data,
                OrganizationNotificationEmailSupport.normalizeEmail(branch.getEmail()),
                safe(branch.getPhoneNumber()),
                "branch");
    }

    public void sendAgentCreated(Agent agent, String performedBy) {
        if (agent == null || agent.getOrganization() == null) {
            return;
        }
        Organization organization = agent.getOrganization();
        Map<String, Object> data = baseData(organization, performedBy);
        String fullName = String.join(" ", safe(agent.getFirstName()), safe(agent.getLastName())).trim();
        data.put("agentName", StringUtils.hasText(fullName) ? fullName : "Agent");
        data.put("agentKind", agent.getAgentKind() != null ? agent.getAgentKind().name() : "");
        data.put("agentType", safe(agent.getAgentType()));
        data.put("agentRole", safe(agent.getRole()));
        data.put("agentEmail", OrganizationNotificationEmailSupport.normalizeEmail(agent.getEmail()));
        data.put("agentPhoneNumber", safe(agent.getPhoneNumber()));
        sendToOrganizationWithExtraEmail(
                organization,
                TEMPLATE_AGENT_CREATED,
                data,
                OrganizationNotificationEmailSupport.normalizeEmail(agent.getEmail()),
                safe(agent.getPhoneNumber()),
                "agent");
    }

    public void sendCustomerLinked(Organization supplier, Organization customer, String performedBy) {
        if (supplier == null || customer == null) {
            return;
        }
        Map<String, Object> supplierData = linkDataForRecipient(supplier, performedBy);
        supplierData.put("supplierName", safe(supplier.getName()));
        supplierData.put("customerName", safe(customer.getName()));
        supplierData.put("linkedOrganizationName", safe(customer.getName()));
        supplierData.put("linkedOrganizationEmail", OrganizationNotificationEmailSupport.normalizeEmail(customer.getEmail()));
        supplierData.put("linkPerspective", "supplier");
        sendToOrganizationMandatoryEmails(supplier, TEMPLATE_CUSTOMER_LINKED, supplierData);

        Map<String, Object> customerData = linkDataForRecipient(customer, performedBy);
        customerData.put("supplierName", safe(supplier.getName()));
        customerData.put("customerName", safe(customer.getName()));
        customerData.put("linkedOrganizationName", safe(supplier.getName()));
        customerData.put("linkedOrganizationEmail", OrganizationNotificationEmailSupport.normalizeEmail(supplier.getEmail()));
        customerData.put("linkPerspective", "customer");
        sendToOrganizationMandatoryEmails(customer, TEMPLATE_CUSTOMER_LINKED, customerData);
    }

    public void sendClearingAgentLinked(Organization supplier, Organization clearingAgent, String performedBy) {
        if (supplier == null || clearingAgent == null) {
            return;
        }
        Map<String, Object> supplierData = linkDataForRecipient(supplier, performedBy);
        supplierData.put("supplierName", safe(supplier.getName()));
        supplierData.put("clearingAgentName", safe(clearingAgent.getName()));
        supplierData.put("linkedOrganizationName", safe(clearingAgent.getName()));
        supplierData.put("linkedOrganizationEmail", OrganizationNotificationEmailSupport.normalizeEmail(clearingAgent.getEmail()));
        supplierData.put("linkPerspective", "supplier");
        sendToOrganizationMandatoryEmails(supplier, TEMPLATE_CLEARING_AGENT_LINKED, supplierData);

        Map<String, Object> agentData = linkDataForRecipient(clearingAgent, performedBy);
        agentData.put("supplierName", safe(supplier.getName()));
        agentData.put("clearingAgentName", safe(clearingAgent.getName()));
        agentData.put("linkedOrganizationName", safe(supplier.getName()));
        agentData.put("linkedOrganizationEmail", OrganizationNotificationEmailSupport.normalizeEmail(supplier.getEmail()));
        agentData.put("linkPerspective", "clearing-agent");
        sendToOrganizationMandatoryEmails(clearingAgent, TEMPLATE_CLEARING_AGENT_LINKED, agentData);
    }

    public void sendTransporterLinked(Organization supplier, Organization transporter, String performedBy) {
        if (supplier == null || transporter == null) {
            return;
        }
        Map<String, Object> supplierData = linkDataForRecipient(supplier, performedBy);
        supplierData.put("supplierName", safe(supplier.getName()));
        supplierData.put("transporterName", safe(transporter.getName()));
        supplierData.put("linkedOrganizationName", safe(transporter.getName()));
        supplierData.put("linkedOrganizationEmail", OrganizationNotificationEmailSupport.normalizeEmail(transporter.getEmail()));
        supplierData.put("linkPerspective", "supplier");
        sendToOrganizationMandatoryEmails(supplier, TEMPLATE_TRANSPORTER_LINKED, supplierData);

        Map<String, Object> transporterData = linkDataForRecipient(transporter, performedBy);
        transporterData.put("supplierName", safe(supplier.getName()));
        transporterData.put("transporterName", safe(transporter.getName()));
        transporterData.put("linkedOrganizationName", safe(supplier.getName()));
        transporterData.put("linkedOrganizationEmail", OrganizationNotificationEmailSupport.normalizeEmail(supplier.getEmail()));
        transporterData.put("linkPerspective", "transporter");
        sendToOrganizationMandatoryEmails(transporter, TEMPLATE_TRANSPORTER_LINKED, transporterData);
    }

    /**
     * Notify a transporter that a supplier has offered to contract them: email to the transporter org plus an
     * in-app bell (action route to the connection-requests page) to each of the transporter's platform users.
     */
    public void sendTransporterLinkOffer(Organization supplier, Organization transporter, String performedBy) {
        if (supplier == null || transporter == null) {
            return;
        }
        Map<String, Object> data = linkDataForRecipient(transporter, performedBy);
        data.put("supplierName", safe(supplier.getName()));
        data.put("transporterName", safe(transporter.getName()));
        data.put("linkedOrganizationName", safe(supplier.getName()));
        data.put("linkedOrganizationEmail", OrganizationNotificationEmailSupport.normalizeEmail(supplier.getEmail()));
        data.put("linkPerspective", "transporter");
        data.put("signInLink", portalLinks.adminSignInUrl());
        sendToOrganizationMandatoryEmails(transporter, TEMPLATE_TRANSPORTER_OFFER, data);

        String title = "New transporter contract offer";
        String body = safe(supplier.getName()) + " has invited you to provide transportation services.";
        publishBellToOrganizationUsers(
                transporter.getId(),
                supplier.getId(),
                EVENT_TRANSPORTER_OFFER,
                title,
                body,
                CONNECTION_REQUESTS_ROUTE,
                ENTITY_TRANSPORTER_OFFER,
                supplier.getId());
    }

    /** Notify the supplier's users that the transporter accepted their offer (the email follows via sendTransporterLinked). */
    public void sendTransporterOfferAccepted(Organization supplier, Organization transporter) {
        if (supplier == null || transporter == null) {
            return;
        }
        String title = "Transporter accepted your offer";
        String body = safe(transporter.getName()) + " accepted your contract offer and is now a linked transporter.";
        publishBellToOrganizationUsers(
                supplier.getId(),
                transporter.getId(),
                EVENT_TRANSPORTER_OFFER_ACCEPTED,
                title,
                body,
                TRANSPORTERS_ROUTE,
                ENTITY_TRANSPORTER_OFFER,
                transporter.getId());
    }

    private void publishBellToOrganizationUsers(
            Long recipientOrganizationId,
            Long counterpartyOrganizationId,
            String eventKey,
            String title,
            String body,
            String actionRoute,
            String entityType,
            Long entityId) {
        if (recipientOrganizationId == null) {
            return;
        }
        for (UserDto user : loadOrganizationUsers(recipientOrganizationId)) {
            if (user == null || user.getId() == null) {
                continue;
            }
            PlatformBellNotificationRequest request = new PlatformBellNotificationRequest(
                    eventKey + ":" + entityType + ":" + entityId + ":" + user.getId(),
                    user.getId(),
                    recipientOrganizationId,
                    eventKey,
                    title,
                    body,
                    actionRoute,
                    entityType,
                    entityId,
                    SOURCE_SERVICE);
            platformBellNotificationPublisher.publish(request);
        }
    }

    private List<UserDto> loadOrganizationUsers(Long organizationId) {
        try {
            UserResponse response = userManagementServiceClient.findByOrganizationId(organizationId);
            if (response != null && response.isSuccess() && response.getUserDtoList() != null) {
                return response.getUserDtoList();
            }
        } catch (Exception ex) {
            log.warn("Failed loading users for organisation {} for bell notification: {}", organizationId, ex.getMessage());
        }
        return List.of();
    }

    private Map<String, Object> linkDataForRecipient(Organization recipient, String performedBy) {
        return baseData(recipient, performedBy);
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
        data.put("signInLink", portalLinks.adminSignInUrl());
        return data;
    }

    private void sendToOrganizationMandatoryEmails(Organization organization, String templateKey, Map<String, Object> data) {
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
                    "Publishing organization directory notification template={} role={} to={}",
                    templateKey,
                    recipientRole,
                    normalizedEmail);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, request);
        } catch (Exception e) {
            log.error(
                    "Failed publishing organization directory notification template={} role={} to={}: {}",
                    templateKey,
                    recipientRole,
                    normalizedEmail,
                    e.getMessage());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
