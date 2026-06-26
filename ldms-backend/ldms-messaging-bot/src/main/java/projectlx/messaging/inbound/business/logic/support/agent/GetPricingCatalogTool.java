package projectlx.messaging.inbound.business.logic.support.agent;

import com.fasterxml.jackson.databind.JsonNode;
import projectlx.messaging.inbound.business.logic.support.BotAgentExecutionContext;
import projectlx.messaging.inbound.business.logic.support.BotAgentJsonSupport;
import projectlx.messaging.inbound.business.logic.support.BotAgentTool;
import projectlx.messaging.inbound.business.logic.support.BotAgentToolRegistry;
import projectlx.messaging.inbound.clients.BillingPaymentsServiceClient;

import java.util.Map;

public class GetPricingCatalogTool implements BotAgentTool {

    private final BillingPaymentsServiceClient billingPaymentsServiceClient;

    public GetPricingCatalogTool(BillingPaymentsServiceClient billingPaymentsServiceClient) {
        this.billingPaymentsServiceClient = billingPaymentsServiceClient;
    }

    @Override
    public String name() {
        return "get_pricing_catalog";
    }

    @Override
    public String description() {
        return "List active LDMS platform action charges (wallet fees per workflow step).";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of();
    }

    @Override
    public String execute(BotAgentExecutionContext context, Map<String, Object> arguments) {
        JsonNode response = billingPaymentsServiceClient.getPublicPricingCatalog(BotAgentToolRegistry.locale(context));
        return BotAgentJsonSupport.pricingCatalogText(response);
    }
}
