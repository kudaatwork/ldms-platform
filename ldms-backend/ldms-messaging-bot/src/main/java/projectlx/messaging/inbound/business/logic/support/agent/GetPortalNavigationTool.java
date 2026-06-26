package projectlx.messaging.inbound.business.logic.support.agent;

import projectlx.messaging.inbound.business.logic.support.BotAgentExecutionContext;
import projectlx.messaging.inbound.business.logic.support.BotAgentTool;
import projectlx.messaging.inbound.business.logic.support.BotAgentToolRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GetPortalNavigationTool implements BotAgentTool {

    private static final Map<String, List<String>> ROUTES_BY_CLASSIFICATION = Map.of(
            "SUPPLIER", List.of(
                    "Dashboard → /dashboard",
                    "Inventory → orders, products, warehouses, stock",
                    "Shipment Management → dispatch, shipments, ready-for-release",
                    "Fleet → trucks, drivers, compliance",
                    "Customers → /customers",
                    "Documents → /documents",
                    "Billing → Settings → Billing (/settings?section=billing)",
                    "Analytics → usage and trip reports",
                    "Users & groups → /users",
                    "Help & Support → /help"),
            "CUSTOMER", List.of(
                    "Dashboard → /dashboard",
                    "My Orders → purchase orders and requisitions",
                    "Shipment Management → track inbound shipments",
                    "Fleet & Transport → assigned transport visibility",
                    "Invoices → /invoices",
                    "Documents → /documents",
                    "Billing → Settings → Billing",
                    "Analytics → /analytics",
                    "Help & Support → /help"),
            "TRANSPORT_COMPANY", List.of(
                    "Dashboard → /dashboard",
                    "Fleet → trucks, drivers, compliance documents",
                    "Shipment Management → assigned shipments and dispatches",
                    "Documents → /documents",
                    "Billing → Settings → Billing",
                    "Analytics → trip and usage reports",
                    "Help & Support → /help"),
            "CLEARING_AGENT", List.of(
                    "Dashboard → /dashboard",
                    "Shipment Management → border clearance workspace",
                    "Documents → /documents",
                    "Billing → Settings → Billing",
                    "Help & Support → /help"),
            "SERVICE_STATION", List.of(
                    "Dashboard → /dashboard",
                    "Truck Visits → /truck-visits",
                    "Fuel Log → /fuel-log",
                    "Billing → Settings → Billing",
                    "Help & Support → /help"),
            "ROADSIDE_SUPPORT_SERVICE", List.of(
                    "Dashboard → /dashboard",
                    "Incidents → /incidents",
                    "Service Log → /service-log",
                    "Help & Support → /help"),
            "GOVERNMENT_AGENCY", List.of(
                    "Dashboard → /dashboard",
                    "Border Activity → /border-activity",
                    "Documents → /documents",
                    "Help & Support → /help")
    );

    @Override
    public String name() {
        return "get_portal_navigation";
    }

    @Override
    public String description() {
        return "Return platform portal menu paths for the user's organisation classification (supplier, customer, transporter, etc.).";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("classification", BotAgentToolRegistry.enumParam(
                "Optional organisation classification override (SUPPLIER, CUSTOMER, TRANSPORT_COMPANY, CLEARING_AGENT, SERVICE_STATION, ROADSIDE_SUPPORT_SERVICE, GOVERNMENT_AGENCY).",
                List.of("SUPPLIER", "CUSTOMER", "TRANSPORT_COMPANY", "CLEARING_AGENT",
                        "SERVICE_STATION", "ROADSIDE_SUPPORT_SERVICE", "GOVERNMENT_AGENCY"),
                false));
        return schema;
    }

    @Override
    public String execute(BotAgentExecutionContext context, Map<String, Object> arguments) {
        String classification = BotAgentToolRegistry.readString(arguments, "classification");
        if (classification.isBlank()) {
            classification = context.organizationClassification() == null ? "" : context.organizationClassification();
        }
        if (classification.isBlank()) {
            return "Organisation classification is unknown. Call get_session_context first.";
        }
        List<String> routes = ROUTES_BY_CLASSIFICATION.get(classification.toUpperCase());
        if (routes == null) {
            return "No navigation map for classification " + classification
                    + ". Use LDMS knowledge docs for generic guidance.";
        }
        StringBuilder out = new StringBuilder("Platform portal navigation for ").append(classification).append(":\n");
        for (String route : routes) {
            out.append("- ").append(route).append('\n');
        }
        return out.toString().trim();
    }
}
