package projectlx.inventory.management.service.processor.api;

import projectlx.inventory.management.utils.requests.DispatchIngestRequest;
import projectlx.inventory.management.utils.requests.GrvCallbackRequest;
import projectlx.inventory.management.utils.responses.CrossDockDispatchResponse;

import java.util.Locale;

public interface CrossDockDispatchServiceProcessor {

    CrossDockDispatchResponse ingestDispatch(DispatchIngestRequest request, Locale locale);

    CrossDockDispatchResponse handleGrvCallback(GrvCallbackRequest request, Locale locale);

    CrossDockDispatchResponse findById(Long id, Locale locale, String username);

    CrossDockDispatchResponse findAllByOrganization(Long organizationId, Locale locale, String username);
}
