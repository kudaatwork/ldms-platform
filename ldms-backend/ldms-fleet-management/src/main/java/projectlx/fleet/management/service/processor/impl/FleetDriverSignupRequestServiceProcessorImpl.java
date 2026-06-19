package projectlx.fleet.management.service.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import projectlx.fleet.management.business.logic.api.FleetDriverSignupRequestService;
import projectlx.fleet.management.service.processor.api.FleetDriverSignupRequestServiceProcessor;
import projectlx.fleet.management.utils.enums.DriverSignupDocumentSlot;
import projectlx.fleet.management.utils.requests.CreateFleetDriverSignupRequest;
import projectlx.fleet.management.utils.responses.FleetDriverSignupRequestResponse;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@RequiredArgsConstructor
@Slf4j
public class FleetDriverSignupRequestServiceProcessorImpl implements FleetDriverSignupRequestServiceProcessor {

    private final FleetDriverSignupRequestService fleetDriverSignupRequestService;

    @Override
    public FileUploadResponse uploadSignupDocument(Long stagingSessionId, DriverSignupDocumentSlot slot,
                                                   MultipartFile file, Locale locale) {
        return fleetDriverSignupRequestService.uploadSignupDocument(stagingSessionId, slot, file, locale);
    }

    @Override
    public FleetDriverSignupRequestResponse submitSignupRequest(CreateFleetDriverSignupRequest request, Locale locale) {
        log.info("Processing driver signup request for email={}", request != null ? request.getEmail() : null);
        return fleetDriverSignupRequestService.submitSignupRequest(request, locale);
    }

    @Override
    public FleetDriverSignupRequestResponse listPendingForOrganization(Locale locale, String username) {
        return fleetDriverSignupRequestService.listPendingForOrganization(locale, username);
    }

    @Override
    public FleetDriverSignupRequestResponse listFreelanceMarketplace(Locale locale) {
        return fleetDriverSignupRequestService.listFreelanceMarketplace(locale);
    }

    @Override
    public FleetDriverSignupRequestResponse approveSignupRequest(Long id, Locale locale, String username) {
        return fleetDriverSignupRequestService.approveSignupRequest(id, locale, username);
    }

    @Override
    public FleetDriverSignupRequestResponse rejectSignupRequest(Long id, Locale locale, String username) {
        return fleetDriverSignupRequestService.rejectSignupRequest(id, locale, username);
    }
}
