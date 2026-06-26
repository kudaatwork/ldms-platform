package projectlx.messaging.inbound.business.logic.support;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BotAgentJsonSupport {

    private BotAgentJsonSupport() {
    }

    public static String walletSummaryText(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return "Wallet summary is unavailable.";
        }
        if (!root.path("success").asBoolean(false)) {
            return "Wallet summary request failed: " + root.path("message").asText("unknown error");
        }
        JsonNode summary = root.path("platformWalletSummaryDto");
        if (summary.isMissingNode() || summary.isNull()) {
            return "No wallet record found for this organisation.";
        }
        long balanceCents = summary.path("balanceCents").asLong(0);
        String currency = summary.path("currencyCode").asText("USD");
        String billingMode = summary.path("billingMode").asText("—");
        String packageName = summary.path("subscriptionPackageName").asText("");
        boolean lowBalance = summary.path("lowBalance").asBoolean(false);
        boolean frozen = summary.path("walletFrozen").asBoolean(false);
        StringBuilder out = new StringBuilder();
        out.append("Balance: ").append(formatMoney(balanceCents)).append(' ').append(currency);
        out.append("\nBilling mode: ").append(billingMode);
        if (!packageName.isBlank()) {
            out.append("\nSubscription package: ").append(packageName);
        }
        if (lowBalance) {
            out.append("\nWarning: wallet is below the low-balance threshold.");
        }
        if (frozen) {
            out.append("\nWarning: wallet is frozen — some actions may be blocked.");
        }
        return out.toString();
    }

    public static String supportTicketsText(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return "Support tickets are unavailable.";
        }
        if (!root.path("success").asBoolean(false)) {
            return "Support ticket list failed: " + root.path("message").asText("unknown error");
        }
        JsonNode tickets = root.path("supportTicketDtoList");
        if (!tickets.isArray() || tickets.isEmpty()) {
            return "No open support tickets for this user.";
        }
        StringBuilder out = new StringBuilder("Support tickets:\n");
        int count = 0;
        for (JsonNode ticket : tickets) {
            if (count >= 10) {
                out.append("- …and more\n");
                break;
            }
            out.append("- #")
                    .append(ticket.path("ticketNumber").asText("—"))
                    .append(" [")
                    .append(ticket.path("status").asText("—"))
                    .append("] ")
                    .append(ticket.path("subject").asText("—"))
                    .append('\n');
            count++;
        }
        return out.toString().trim();
    }

    public static String createdTicketText(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return "Support ticket creation returned no response.";
        }
        if (!root.path("success").asBoolean(false)) {
            return "Could not create support ticket: " + root.path("message").asText("unknown error");
        }
        JsonNode ticket = root.path("supportTicketDto");
        if (ticket.isMissingNode() || ticket.isNull()) {
            return "Support ticket may have been created but no ticket details were returned.";
        }
        return "Created support ticket #"
                + ticket.path("ticketNumber").asText("—")
                + " — "
                + ticket.path("subject").asText("—")
                + " (status: "
                + ticket.path("status").asText("OPEN")
                + "). The user can view it under Help & Support → My tickets.";
    }

    public static String pricingCatalogText(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return "Pricing catalog is unavailable.";
        }
        if (!root.path("success").asBoolean(false)) {
            return "Pricing catalog request failed: " + root.path("message").asText("unknown error");
        }
        JsonNode charges = root.path("platformActionChargeDtoList");
        if (!charges.isArray() || charges.isEmpty()) {
            return "No active platform action charges found.";
        }
        StringBuilder out = new StringBuilder("Platform action charges (USD):\n");
        int count = 0;
        for (JsonNode charge : charges) {
            if (count >= 20) {
                out.append("- …and more\n");
                break;
            }
            long cents = charge.path("chargeCents").asLong(0);
            out.append("- ")
                    .append(charge.path("actionCode").asText("—"))
                    .append(": ")
                    .append(charge.path("displayName").asText("—"))
                    .append(" — ")
                    .append(formatMoney(cents))
                    .append('\n');
            count++;
        }
        return out.toString().trim();
    }

    public static String userGroupsText(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return "User groups are unavailable.";
        }
        if (!root.path("success").asBoolean(false)) {
            return "User group list failed: " + root.path("message").asText("unknown error");
        }
        JsonNode groups = root.path("userGroupDtoList");
        if (!groups.isArray() || groups.isEmpty()) {
            JsonNode page = root.path("userGroupDtoPage").path("content");
            if (page.isArray() && !page.isEmpty()) {
                groups = page;
            }
        }
        if (!groups.isArray() || groups.isEmpty()) {
            return "No user groups found for this organisation.";
        }
        StringBuilder out = new StringBuilder("User groups:\n");
        int count = 0;
        for (JsonNode group : groups) {
            if (count >= 15) {
                out.append("- …and more\n");
                break;
            }
            out.append("- id=")
                    .append(group.path("id").asText("—"))
                    .append(" | ")
                    .append(group.path("name").asText("—"));
            long members = group.path("userMemberCount").asLong(0);
            if (members > 0) {
                out.append(" (").append(members).append(" members)");
            }
            if (group.path("systemGroup").asBoolean(false)) {
                out.append(" [system]");
            }
            out.append('\n');
            count++;
        }
        return out.toString().trim();
    }

    public static String createdUserGroupText(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return "User group creation returned no response.";
        }
        if (!root.path("success").asBoolean(false)) {
            return "Could not create user group: " + root.path("message").asText("unknown error");
        }
        JsonNode group = root.path("userGroupDto");
        if (group.isMissingNode() || group.isNull()) {
            return "User group may have been created but no details were returned.";
        }
        return "Created user group **"
                + group.path("name").asText("—")
                + "** (id="
                + group.path("id").asText("—")
                + "). Manage it under Settings → Users → User groups.";
    }

    public static String orgUsersText(JsonNode root) {
        if (root == null || root.isMissingNode()) {
            return "Organisation users are unavailable.";
        }
        if (!root.path("success").asBoolean(false)) {
            return "User list failed: " + root.path("message").asText("unknown error");
        }
        JsonNode users = root.path("userDtoList");
        if (!users.isArray() || users.isEmpty()) {
            JsonNode page = root.path("userDtoPage").path("content");
            if (page.isArray() && !page.isEmpty()) {
                users = page;
            }
        }
        if (!users.isArray() || users.isEmpty()) {
            return "No users found in this organisation workspace.";
        }
        StringBuilder out = new StringBuilder("Organisation users:\n");
        int count = 0;
        for (JsonNode user : users) {
            if (count >= 20) {
                out.append("- …and more\n");
                break;
            }
            out.append("- id=")
                    .append(user.path("id").asText("—"))
                    .append(" | ")
                    .append(user.path("username").asText("—"));
            String first = user.path("firstName").asText("");
            String last = user.path("lastName").asText("");
            if (!first.isBlank() || !last.isBlank()) {
                out.append(" | ").append((first + " " + last).trim());
            }
            String email = user.path("email").asText("");
            if (!email.isBlank()) {
                out.append(" | ").append(email);
            }
            out.append('\n');
            count++;
        }
        return out.toString().trim();
    }

    public static String addUsersToGroupText(JsonNode root, long userGroupId, int userCount) {
        if (root == null || root.isMissingNode()) {
            return "Add users request returned no response.";
        }
        if (!root.path("success").asBoolean(false)) {
            return "Could not add users to group: " + root.path("message").asText("unknown error");
        }
        JsonNode group = root.path("userGroupDto");
        String groupName = group.path("name").asText("group #" + userGroupId);
        return "Added " + userCount + " user(s) to **" + groupName + "** (id=" + userGroupId + ").";
    }

    private static String formatMoney(long cents) {
        return BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
