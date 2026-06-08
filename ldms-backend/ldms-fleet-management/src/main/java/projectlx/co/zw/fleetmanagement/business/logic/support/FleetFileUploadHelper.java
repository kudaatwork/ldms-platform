package projectlx.co.zw.fleetmanagement.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import projectlx.co.zw.fleetmanagement.clients.FileUploadServiceClient;
import projectlx.co.zw.fleetmanagement.utils.enums.ComplianceSubjectType;
import projectlx.co.zw.fleetmanagement.utils.enums.I18Code;
import projectlx.co.zw.shared_library.utils.dtos.FileUploadDto;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

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

    public LocalDateTime resolveExpiresAt(LocalDateTime expiresAt, Long fileUploadId) {
        if (expiresAt != null) {
            return expiresAt;
        }
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
