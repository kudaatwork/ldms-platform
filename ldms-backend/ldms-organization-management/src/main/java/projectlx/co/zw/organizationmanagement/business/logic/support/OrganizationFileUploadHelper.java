package projectlx.co.zw.organizationmanagement.business.logic.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.organizationmanagement.clients.FileUploadServiceClient;
import projectlx.co.zw.organizationmanagement.model.Organization;
import projectlx.co.zw.organizationmanagement.utils.enums.I18Code;
import projectlx.co.zw.shared_library.utils.dtos.FileUploadDto;
import projectlx.co.zw.shared_library.utils.enums.FileType;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.requests.SingleFileUploadRequest;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongConsumer;

/**
 * Uploads organisation documents via file-upload-service (same pattern as user national ID in user-management).
 */
@Component
@RequiredArgsConstructor
public class OrganizationFileUploadHelper {

    private static final Logger log = LoggerFactory.getLogger(OrganizationFileUploadHelper.class);

    private final FileUploadServiceClient fileUploadServiceClient;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;

    public record UploadOutcome(boolean success, List<String> errorMessages) {
        public static UploadOutcome ok() {
            return new UploadOutcome(true, List.of());
        }

        public static UploadOutcome failed(List<String> errors) {
            return new UploadOutcome(false, errors);
        }
    }

    public record UploadPart(
            MultipartFile multipart,
            Long preassignedId,
            FileType fileType,
            Long existingRowId,
            LongConsumer assignUploadId,
            String expiresAt) {

        public UploadPart(
                MultipartFile multipart,
                Long preassignedId,
                FileType fileType,
                Long existingRowId,
                LongConsumer assignUploadId) {
            this(multipart, preassignedId, fileType, existingRowId, assignUploadId, null);
        }
    }

    /**
     * Applies pre-assigned upload ids, then uploads any multipart files for the organisation owner.
     */
    public UploadOutcome processUploads(Organization org, List<UploadPart> parts, Locale locale, boolean isUpdate) {
        for (UploadPart part : parts) {
            if (part.preassignedId() != null) {
                part.assignUploadId().accept(part.preassignedId());
            }
        }

        List<UploadPart> pending = parts.stream()
                .filter(p -> p.multipart() != null && !p.multipart().isEmpty())
                .toList();
        if (pending.isEmpty()) {
            return UploadOutcome.ok();
        }

        try {
            List<SingleFileUploadRequest> metadata = new ArrayList<>();
            for (UploadPart part : pending) {
                SingleFileUploadRequest single = new SingleFileUploadRequest();
                if (isUpdate && part.existingRowId() != null) {
                    single.setId(part.existingRowId());
                }
                single.setFile(part.multipart());
                single.setFileType(part.fileType().name());
                if (part.expiresAt() != null && !part.expiresAt().isBlank()) {
                    single.setExpiresAt(parseExpiryStringToLocalDateTime(part.expiresAt()));
                }
                metadata.add(single);
            }

            List<MultipartFile> files = metadata.stream().map(SingleFileUploadRequest::getFile).toList();
            List<Map<String, Object>> metadataList = metadata.stream()
                    .map(m -> {
                        Map<String, Object> map = new HashMap<>();
                        if (m.getId() != null) {
                            map.put("id", m.getId());
                        }
                        map.put("fileType", m.getFileType());
                        map.put("expiresAt", m.getExpiresAt());
                        return map;
                    })
                    .toList();

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("filesMetadata", metadataList);
            requestMap.put("ownerType", OwnerType.ORGANIZATION.getOwnerType());
            requestMap.put("ownerId", org.getId());

            String json = objectMapper.writeValueAsString(requestMap);
            FileUploadResponse response = isUpdate
                    ? fileUploadServiceClient.update(files, json)
                    : fileUploadServiceClient.upload(files, json);

            if (response == null || !response.isSuccess()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                String message = messageService.getMessage(I18Code.ORG_DOCUMENT_UPLOAD_FAILED.getCode(), new String[]{}, locale);
                List<String> errors = new ArrayList<>();
                if (response != null && response.getMessage() != null && !response.getMessage().isBlank()) {
                    errors.add(response.getMessage());
                } else {
                    errors.add(message);
                }
                return UploadOutcome.failed(errors);
            }

            List<FileUploadDto> uploaded = new ArrayList<>();
            if (response.getFileUploadDtoList() != null) {
                uploaded.addAll(response.getFileUploadDtoList());
            } else if (response.getFileUploadDto() != null) {
                uploaded.add(response.getFileUploadDto());
            }

            Map<FileType, LongConsumer> assigners = assignerMap(org);
            for (FileUploadDto dto : uploaded) {
                if (dto.getFileType() == null || dto.getId() == null) {
                    continue;
                }
                LongConsumer assigner = assigners.get(dto.getFileType());
                if (assigner != null) {
                    assigner.accept(dto.getId());
                }
            }

            for (UploadPart part : pending) {
                if (part.preassignedId() != null) {
                    continue;
                }
                if (existingId(org, part.fileType()) == null) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    String message = messageService.getMessage(I18Code.ORG_DOCUMENT_UPLOAD_FAILED.getCode(), new String[]{}, locale);
                    return UploadOutcome.failed(List.of(message + " (" + part.fileType().name() + ")"));
                }
            }

            return UploadOutcome.ok();
        } catch (Exception e) {
            log.error("Organisation document upload failed", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            String message = messageService.getMessage(I18Code.ORG_DOCUMENT_UPLOAD_FAILED.getCode(), new String[]{}, locale);
            return UploadOutcome.failed(List.of(message));
        }
    }

    private static LocalDateTime parseExpiryStringToLocalDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        }
    }

    private Map<FileType, LongConsumer> assignerMap(Organization org) {
        Map<FileType, LongConsumer> map = new HashMap<>();
        map.put(FileType.TAX_CLEARANCE_CERTIFICATE, org::setTaxClearanceCertificateUploadId);
        map.put(FileType.COMPANY_REGISTRATION_CERTIFICATE, org::setRegistrationCertificateUploadId);
        map.put(FileType.BUSINESS_LICENSE, org::setBusinessLicenseUploadId);
        map.put(FileType.PROOF_OF_ADDRESS, org::setProofOfAddressUploadId);
        map.put(FileType.INDUSTRY_SPECIFIC_LICENSE, org::setIndustrySpecificLicenseUploadId);
        map.put(FileType.ORGANIZATION_LOGO, org::setLogoUploadId);
        map.put(FileType.NATIONAL_ID, org::setContactPersonNationalIdUploadId);
        map.put(FileType.PASSPORT, org::setContactPersonPassportUploadId);
        return map;
    }

    public Long existingId(Organization org, FileType type) {
        return switch (type) {
            case TAX_CLEARANCE_CERTIFICATE -> org.getTaxClearanceCertificateUploadId();
            case COMPANY_REGISTRATION_CERTIFICATE -> org.getRegistrationCertificateUploadId();
            case BUSINESS_LICENSE -> org.getBusinessLicenseUploadId();
            case PROOF_OF_ADDRESS -> org.getProofOfAddressUploadId();
            case INDUSTRY_SPECIFIC_LICENSE -> org.getIndustrySpecificLicenseUploadId();
            case ORGANIZATION_LOGO -> org.getLogoUploadId();
            case NATIONAL_ID -> org.getContactPersonNationalIdUploadId();
            case PASSPORT -> org.getContactPersonPassportUploadId();
            default -> null;
        };
    }
}
