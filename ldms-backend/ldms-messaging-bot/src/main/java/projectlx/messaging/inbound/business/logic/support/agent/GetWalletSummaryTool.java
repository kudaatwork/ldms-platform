package projectlx.messaging.inbound.business.logic.support.agent;

import com.fasterxml.jackson.databind.JsonNode;
import projectlx.messaging.inbound.business.logic.support.BotAgentExecutionContext;
import projectlx.messaging.inbound.business.logic.support.BotAgentJsonSupport;
import projectlx.messaging.inbound.business.logic.support.BotAgentTool;
import projectlx.messaging.inbound.business.logic.support.BotAgentToolRegistry;
import projectlx.messaging.inbound.clients.BillingPaymentsServiceClient;

import java.util.Map;

public class GetWalletSummaryTool implements BotAgentTool {

    private final BillingPaymentsServiceClient billingPaymentsServiceClient;

    public GetWalletSummaryTool(BillingPaymentsServiceClient billingPaymentsServiceClient) {
        this.billingPaymentsServiceClient = billingPaymentsServiceClient;
    }

    @Override
    public String name() {
        return "get_wallet_summary";
    }

    @Override
    public String description() {
        return "Fetch the current organisation platform wallet balance, billing mode, and subscription package.";
    }

    @Override
    public Map<String, Object> parameterSchema() {
        return Map.of();
    }

    @Override
    public String execute(BotAgentExecutionContext context, Map<String, Object> arguments) {
        if (context.organizationId() == null) {
            return "No organisation is linked to this user — wallet summary is unavailable.";
        }
        JsonNode response = billingPaymentsServiceClient.getWalletSummary(
                context.organizationId(), BotAgentToolRegistry.locale(context));
        return BotAgentJsonSupport.walletSummaryText(response);
    }
}
