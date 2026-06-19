package projectlx.fleet.management.service.processor.api;

import projectlx.fleet.management.utils.enums.DriverSignupDocumentSlot;
import projectlx.fleet.management.utils.requests.CreateFleetDriverSignupRequest;
import projectlx.fleet.management.utils.responses.FleetDriverSignupRequestResponse;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

public interface FleetDriverSignupRequestServiceProcessor {

    FileUploadResponse uploadSignupDocument(Long stagingSessionId, DriverSignupDocumentSlot slot,
                                            MultipartFile file, Locale locale);

    FleetDriverSignupRequestResponse submitSignupRequest(CreateFleetDriverSignupRequest request, Locale locale);

    FleetDriverSignupRequestResponse listPendingForOrganization(Locale locale, String username);

    FleetDriverSignupRequestResponse listFreelanceMarketplace(Locale locale);

    FleetDriverSignupRequestResponse approveSignupRequest(Long id, Locale locale, String username);

    FleetDriverSignupRequestResponse rejectSignupRequest(Long id, Locale locale, String username);
}
