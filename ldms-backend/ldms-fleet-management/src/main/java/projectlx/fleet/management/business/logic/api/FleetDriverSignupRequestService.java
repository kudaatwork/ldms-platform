package projectlx.fleet.management.business.logic.api;

import projectlx.fleet.management.utils.enums.DriverSignupDocumentSlot;
import projectlx.fleet.management.utils.requests.CreateFleetDriverSignupRequest;
import projectlx.fleet.management.utils.responses.FleetDriverSignupRequestResponse;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

public interface FleetDriverSignupRequestService {

    FileUploadResponse uploadSignupDocument(Long stagingSessionId, DriverSignupDocumentSlot slot,
                                            MultipartFile file, Locale locale);

    /**
     * Submits a new driver signup request (public endpoint, no auth).
     * When {@code companyCode} is blank the request type is {@code FREELANCE};
     * otherwise {@code COMPANY}.
     */
    FleetDriverSignupRequestResponse submitSignupRequest(CreateFleetDriverSignupRequest request, Locale locale);

    /**
     * Lists PENDING COMPANY signup requests for the caller's organisation.
     * Used by transport-company operators on the platform portal.
     *
     * @param username caller's username (used to resolve their organisation)
     */
    FleetDriverSignupRequestResponse listPendingForOrganization(Locale locale, String username);

    /**
     * Lists all PENDING FREELANCE signup requests visible across the marketplace.
     * Any authenticated transporter can see these.
     */
    FleetDriverSignupRequestResponse listFreelanceMarketplace(Locale locale);

    /**
     * Approves a signup request and provisions driver + platform user.
     * <ul>
     *   <li>COMPANY → resolves org from {@code companyCode} (numeric string = orgId for MVP).</li>
     *   <li>FREELANCE → uses the caller's organisation.</li>
     * </ul>
     *
     * @param id       signup request id
     * @param locale   request locale
     * @param username caller's username
     */
    FleetDriverSignupRequestResponse approveSignupRequest(Long id, Locale locale, String username);

    /**
     * Rejects a pending signup request.
     *
     * @param id       signup request id
     * @param locale   request locale
     * @param username caller's username
     */
    FleetDriverSignupRequestResponse rejectSignupRequest(Long id, Locale locale, String username);
}
