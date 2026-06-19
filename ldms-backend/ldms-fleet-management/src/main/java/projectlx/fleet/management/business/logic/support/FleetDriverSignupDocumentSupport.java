package projectlx.fleet.management.business.logic.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import projectlx.fleet.management.clients.FileUploadServiceClient;
import projectlx.fleet.management.utils.enums.DriverSignupDocumentSlot;
import projectlx.co.zw.shared_library.utils.dtos.FileUploadDto;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class FleetDriverSignupDocumentSupport {

    private final FileUploadServiceClient fileUploadServiceClient;
    private final FleetFileUploadHelper fleetFileUploadHelper;

    public Long uploadStagingDocument(Long stagingSessionId, DriverSignupDocumentSlot slot,
                                      MultipartFile file, Locale locale) {
        if (stagingSessionId == null || stagingSessionId < 1 || slot == null || file == null || file.isEmpty()) {
            return null;
        }
        String requestJson = String.format(
                "{\"ownerType\":\"%s\",\"ownerId\":%d,\"filesMetadata\":[{\"fileType\":\"%s\"}]}",
                OwnerType.FLEET_DRIVER_SIGNUP.name(),
                stagingSessionId,
                slot.getFileType().name());
        FileUploadResponse response = fileUploadServiceClient.upload(List.of(file), requestJson);
        if (response == null || !response.isSuccess()) {
            log.warn("Driver signup document upload failed for stagingSessionId={} slot={}",
                    stagingSessionId, slot);
            return null;
        }
        FileUploadDto dto = response.getFileUploadDto();
        if (dto == null && response.getFileUploadDtoList() != null && !response.getFileUploadDtoList().isEmpty()) {
            dto = response.getFileUploadDtoList().get(0);
        }
        return dto != null ? dto.getId() : null;
    }

    public boolean validateStagingDocument(Long fileUploadId, Long stagingSessionId, Locale locale) {
        return fleetFileUploadHelper.validateSignupDocumentReference(fileUploadId, stagingSessionId, locale);
    }
}
