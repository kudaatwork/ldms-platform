package projectlx.inventory.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.CrossDockDispatchService;
import projectlx.inventory.management.service.processor.api.CrossDockDispatchServiceProcessor;
import projectlx.inventory.management.utils.requests.DispatchIngestRequest;
import projectlx.inventory.management.utils.requests.GrvCallbackRequest;
import projectlx.inventory.management.utils.responses.CrossDockDispatchResponse;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CrossDockDispatchServiceProcessorImpl implements CrossDockDispatchServiceProcessor {

    private final CrossDockDispatchService crossDockDispatchService;
    private static final Logger logger = LoggerFactory.getLogger(CrossDockDispatchServiceProcessorImpl.class);

    @Override
    public CrossDockDispatchResponse ingestDispatch(DispatchIngestRequest request, Locale locale) {

        logger.info("Incoming dispatch ingest request [externalId={}]",
                request != null ? request.getExternalDispatchId() : null);
        CrossDockDispatchResponse response = crossDockDispatchService.ingestDispatch(request, locale);
        logger.info("Outgoing response for dispatch ingest: success={}", response != null && response.isSuccess());
        return response;
    }

    @Override
    public CrossDockDispatchResponse handleGrvCallback(GrvCallbackRequest request, Locale locale) {

        logger.info("Incoming GRV callback [grvId={}]",
                request != null ? request.getGrvId() : null);
        CrossDockDispatchResponse response = crossDockDispatchService.handleGrvCallback(request, locale);
        logger.info("Outgoing response for GRV callback: success={}", response != null && response.isSuccess());
        return response;
    }

    @Override
    public CrossDockDispatchResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find cross-dock dispatch by ID: {} for user: {}", id, username);
        CrossDockDispatchResponse response = crossDockDispatchService.findById(id, locale, username);
        logger.info("Outgoing response for findById dispatch: success={}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public CrossDockDispatchResponse findAllByOrganization(Long organizationId, Locale locale, String username) {

        logger.info("Incoming request to find cross-dock dispatches for org: {} by user: {}",
                organizationId, username);
        CrossDockDispatchResponse response =
                crossDockDispatchService.findAllByOrganization(organizationId, locale, username);
        logger.info("Outgoing response for findAllByOrganization dispatch: success={}",
                response != null && response.isSuccess());
        return response;
    }
}
