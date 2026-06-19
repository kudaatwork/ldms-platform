package projectlx.inventory.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.logic.api.InventoryIntegrationCredentialService;
import projectlx.inventory.management.service.processor.api.InventoryIntegrationCredentialServiceProcessor;
import projectlx.inventory.management.utils.requests.CreateInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.requests.EditInventoryIntegrationCredentialRequest;
import projectlx.inventory.management.utils.responses.InventoryIntegrationCredentialResponse;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InventoryIntegrationCredentialServiceProcessorImpl
        implements InventoryIntegrationCredentialServiceProcessor {

    private final InventoryIntegrationCredentialService inventoryIntegrationCredentialService;
    private static final Logger logger =
            LoggerFactory.getLogger(InventoryIntegrationCredentialServiceProcessorImpl.class);

    @Override
    public InventoryIntegrationCredentialResponse create(
            CreateInventoryIntegrationCredentialRequest request, Locale locale, String username) {

        logger.info("Incoming request to create integration credential for user: {}", username);
        InventoryIntegrationCredentialResponse response =
                inventoryIntegrationCredentialService.create(request, locale, username);
        logger.info("Outgoing response after creating integration credential: success={}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public InventoryIntegrationCredentialResponse update(
            EditInventoryIntegrationCredentialRequest request, Locale locale, String username) {

        logger.info("Incoming request to update integration credential for user: {}", username);
        InventoryIntegrationCredentialResponse response =
                inventoryIntegrationCredentialService.update(request, locale, username);
        logger.info("Outgoing response after updating integration credential: success={}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public InventoryIntegrationCredentialResponse findById(Long id, Locale locale, String username) {

        logger.info("Incoming request to find integration credential by ID: {} for user: {}", id, username);
        InventoryIntegrationCredentialResponse response =
                inventoryIntegrationCredentialService.findById(id, locale, username);
        logger.info("Outgoing response for findById credential: success={}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public InventoryIntegrationCredentialResponse findAllByOrganization(
            Long organizationId, Locale locale, String username) {

        logger.info("Incoming request to find integration credentials for org: {} by user: {}",
                organizationId, username);
        InventoryIntegrationCredentialResponse response =
                inventoryIntegrationCredentialService.findAllByOrganization(organizationId, locale, username);
        logger.info("Outgoing response for findAllByOrganization credentials: success={}",
                response != null && response.isSuccess());
        return response;
    }

    @Override
    public InventoryIntegrationCredentialResponse delete(Long id, Locale locale, String username) {

        logger.info("Incoming request to delete integration credential ID: {} by user: {}", id, username);
        InventoryIntegrationCredentialResponse response =
                inventoryIntegrationCredentialService.delete(id, locale, username);
        logger.info("Outgoing response after deleting integration credential: success={}",
                response != null && response.isSuccess());
        return response;
    }
}
