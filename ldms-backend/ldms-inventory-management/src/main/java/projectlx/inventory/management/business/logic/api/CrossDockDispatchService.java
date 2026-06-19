package projectlx.inventory.management.business.logic.api;

import projectlx.inventory.management.model.InventoryIntegrationCredential;
import projectlx.inventory.management.utils.requests.DispatchIngestRequest;
import projectlx.inventory.management.utils.requests.GrvCallbackRequest;
import projectlx.inventory.management.utils.responses.CrossDockDispatchResponse;

import java.util.Locale;

public interface CrossDockDispatchService {

    /**
     * Ingest an external dispatch request — resolves org by api_key, creates a
     * {@link projectlx.inventory.management.model.CrossDockDispatch} record, and publishes
     * the cross.dock.dispatch.created event.
     */
    CrossDockDispatchResponse ingestDispatch(DispatchIngestRequest request, Locale locale);

    /**
     * Record a GRV callback event and push the payload to the partner's registered callbackGrvUrl.
     * Best-effort HTTP push — failures are logged but do not fail the response.
     */
    CrossDockDispatchResponse handleGrvCallback(GrvCallbackRequest request, Locale locale);

    /**
     * HTTP POST the GRV callback payload to the partner's registered callbackGrvUrl.
     * May be called externally (e.g. from GoodsReceiptProcessorImpl) for cross-dock GRV completions.
     */
    void pushGrvCallbackToPartner(InventoryIntegrationCredential credential, GrvCallbackRequest callbackRequest);

    CrossDockDispatchResponse findById(Long id, Locale locale, String username);

    CrossDockDispatchResponse findAllByOrganization(Long organizationId, Locale locale, String username);
}
