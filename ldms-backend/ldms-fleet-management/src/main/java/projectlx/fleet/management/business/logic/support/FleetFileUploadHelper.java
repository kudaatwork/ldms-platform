package projectlx.fleet.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import projectlx.fleet.management.clients.FileUploadServiceClient;
import projectlx.fleet.management.utils.enums.ComplianceSubjectType;
import projectlx.fleet.management.utils.enums.I18Code;
import projectlx.co.zw.shared_library.utils.dtos.FileUploadDto;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Validates compliance document references against file-upload-service and resolves owner types.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FleetFileUploadHelper {

    private final FileUploadServiceClient fileUploadServiceClient;
    private final MessageService messageService;

    public OwnerType resolveOwnerType(ComplianceSubjectType subjectType) {
        return switch (subjectType) {
            case ASSET -> OwnerType.FLEET_ASSET;
            case DRIVER -> OwnerType.FLEET_DRIVER;
        };
    }

    /**
     * Confirms the file upload exists and belongs to the expected fleet subject owner.
     */
    public boolean validateFileUploadReference(
            Long fileUploadId,
            ComplianceSubjectType subjectType,
            Long subjectId,
            Locale locale) {
        if (fileUploadId == null || fileUploadId < 1) {
            return true;
        }
        try {
            FileUploadResponse response = fileUploadServiceClient.findById(fileUploadId);
            if (response == null || !response.isSuccess()) {
                return false;
            }
            FileUploadDto dto = response.getFileUploadDto();
            if (dto == null) {
                return false;
            }
            OwnerType expectedOwnerType = resolveOwnerType(subjectType);
            if (dto.getOwnerType() == null || !expectedOwnerType.name().equals(dto.getOwnerType().name())) {
                log.warn("File upload {} ownerType mismatch: expected {}, got {}",
                        fileUploadId, expectedOwnerType, dto.getOwnerType());
                return false;
            }
            if (dto.getOwnerId() == null || !dto.getOwnerId().equals(subjectId)) {
                log.warn("File upload {} ownerId mismatch: expected {}, got {}",
                        fileUploadId, subjectId, dto.getOwnerId());
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Failed to validate file upload {}: {}", fileUploadId, ex.getMessage());
            return false;
        }
    }

    public String fileUploadInvalidMessage(Locale locale) {
        return messageService.getMessage(I18Code.MESSAGE_FILE_UPLOAD_INVALID.getCode(), new String[]{}, locale);
    }

    /**
     * Validates a driver personal document upload reference.
     *
     * <p>Accepts the upload when its owner is either:</p>
     * <ul>
     *   <li>{@code FLEET_DRIVER} with ownerId matching {@code fleetDriverId} (update path), or</li>
     *   <li>{@code USER} with ownerId matching {@code linkedUserId} (user-owned upload).</li>
     * </ul>
     *
     * <p>If {@code fileUploadId} is {@code null} or zero the method returns {@code true}
     * (presence is enforced separately in the validator).</p>
     *
     * @param fileUploadId  upload id to validate
     * @param fleetDriverId persisted driver id — may be {@code null} on create
     * @param linkedUserId  linked platform user id — may be {@code null} when no user is linked
     * @param locale        request locale (unused currently, reserved for future messages)
     * @return {@code true} when the reference is valid or absent; {@code false} otherwise
     */
    public boolean validateDriverDocumentReference(Long fileUploadId, Long fleetDriverId,
                                                   Long linkedUserId, Locale locale) {
        if (fileUploadId == null || fileUploadId < 1) {
            return true;
        }
        try {
            FileUploadResponse response = fileUploadServiceClient.findById(fileUploadId);
            if (response == null || !response.isSuccess()) {
                return false;
            }
            FileUploadDto dto = response.getFileUploadDto();
            if (dto == null || dto.getOwnerType() == null || dto.getOwnerId() == null) {
                return false;
            }
            String ownerType = dto.getOwnerType().name();

            if (OwnerType.FLEET_DRIVER.name().equals(ownerType)) {
                if (fleetDriverId != null && fleetDriverId.equals(dto.getOwnerId())) {
                    return true;
                }
                log.warn("File upload {} FLEET_DRIVER ownerId mismatch: expected {}, got {}",
                        fileUploadId, fleetDriverId, dto.getOwnerId());
                return false;
            }

            if (OwnerType.USER.name().equals(ownerType)) {
                if (linkedUserId != null && linkedUserId.equals(dto.getOwnerId())) {
                    return true;
                }
                log.warn("File upload {} USER ownerId mismatch: expected {}, got {}",
                        fileUploadId, linkedUserId, dto.getOwnerId());
                return false;
            }

            log.warn("File upload {} has unexpected ownerType {} for driver document", fileUploadId, ownerType);
            return false;
        } catch (Exception ex) {
            log.warn("Failed to validate driver document upload {}: {}", fileUploadId, ex.getMessage());
            return false;
        }
    }

    /**
     * Validates a staging upload created during public driver signup
     * ({@code FLEET_DRIVER_SIGNUP} owner with {@code stagingSessionId}).
     */
    public boolean validateSignupDocumentReference(Long fileUploadId, Long stagingSessionId, Locale locale) {
        if (fileUploadId == null || fileUploadId < 1) {
            return false;
        }
        if (stagingSessionId == null || stagingSessionId < 1) {
            return false;
        }
        try {
            FileUploadResponse response = fileUploadServiceClient.findById(fileUploadId);
            if (response == null || !response.isSuccess()) {
                return false;
            }
            FileUploadDto dto = response.getFileUploadDto();
            if (dto == null || dto.getOwnerType() == null || dto.getOwnerId() == null) {
                return false;
            }
            if (!OwnerType.FLEET_DRIVER_SIGNUP.name().equals(dto.getOwnerType().name())) {
                log.warn("File upload {} ownerType mismatch for signup staging: expected {}, got {}",
                        fileUploadId, OwnerType.FLEET_DRIVER_SIGNUP, dto.getOwnerType());
                return false;
            }
            if (!stagingSessionId.equals(dto.getOwnerId())) {
                log.warn("File upload {} stagingSessionId mismatch: expected {}, got {}",
                        fileUploadId, stagingSessionId, dto.getOwnerId());
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Failed to validate signup document upload {}: {}", fileUploadId, ex.getMessage());
            return false;
        }
    }

    /** Resolves expiry from an optional calendar date (end of day) or file-upload metadata. */
    public LocalDateTime resolveExpiresAt(LocalDate expiresAt, Long fileUploadId) {
        if (expiresAt != null) {
            return expiresAt.atTime(23, 59, 59);
        }
        return resolveExpiresAtFromFileUpload(fileUploadId);
    }

    public LocalDateTime resolveExpiresAt(LocalDateTime expiresAt, Long fileUploadId) {
        if (expiresAt != null) {
            return expiresAt;
        }
        return resolveExpiresAtFromFileUpload(fileUploadId);
    }

    public LocalDateTime resolveExpiresAtFromFileUpload(Long fileUploadId) {
        if (fileUploadId == null || fileUploadId < 1) {
            return null;
        }
        try {
            FileUploadResponse response = fileUploadServiceClient.findById(fileUploadId);
            if (response != null && response.isSuccess() && response.getFileUploadDto() != null) {
                return response.getFileUploadDto().getExpiresAt();
            }
        } catch (Exception ex) {
            log.debug("Could not resolve expiresAt from file upload {}: {}", fileUploadId, ex.getMessage());
        }
        return null;
    }
}
