package projectlx.co.zw.fileuploadservice.business.logic.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.fileuploadservice.business.auditable.api.FileUploadServiceAuditable;
import projectlx.co.zw.fileuploadservice.business.logic.api.FileUploadService;
import projectlx.co.zw.fileuploadservice.business.validator.api.FileUploadServiceValidator;
import projectlx.co.zw.fileuploadservice.model.FileUpload;
import projectlx.co.zw.fileuploadservice.repository.FileUploadRepository;
import projectlx.co.zw.fileuploadservice.utils.config.FileUploadProperties;
import projectlx.co.zw.fileuploadservice.utils.enums.I18Code;
import projectlx.co.zw.fileuploadservice.utils.json.FeignFileUploadRequestPayload;
import projectlx.co.zw.fileuploadservice.utils.json.FeignMultipartFileMetadataEntry;
import projectlx.co.zw.fileuploadservice.storage.FileStorageService;
import projectlx.co.zw.shared_library.utils.dtos.FileUploadDto;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.enums.FileType;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.enums.StorageProvider;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;
import projectlx.co.zw.shared_library.utils.rustfs.RustFsClient;
import projectlx.co.zw.shared_library.utils.rustfs.RustFsDownloadResponse;
import projectlx.co.zw.shared_library.utils.rustfs.RustFsException;
import projectlx.co.zw.shared_library.utils.rustfs.RustFsUploadResponse;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadServiceImpl.class);

    private final FileUploadServiceValidator fileUploadServiceValidator;
    private final FileUploadServiceAuditable fileUploadServiceAuditable;
    private final FileUploadRepository fileUploadRepository;
    private final RustFsClient rustFsClient;
    private final FileStorageService localFileStorageService;
    private final MessageService messageService;
    private final ModelMapper modelMapper;
    @SuppressWarnings("unused")
    private final FileUploadProperties fileUploadProperties;
    private final ObjectMapper objectMapper;

    @Override
    public FileUploadResponse upload(
            List<MultipartFile> files,
            String fileUploadRequestJson,
            Locale locale,
            String username) {

        ValidatorDto validatorDto = fileUploadServiceValidator.validateUpload(files, fileUploadRequestJson, locale);
        if (validatorDto == null || !Boolean.TRUE.equals(validatorDto.getSuccess())) {
            String message = messageService.getMessage(I18Code.FILE_UPLOAD_FAILED.getCode(), new String[]{}, locale);
            return buildErrorResponse(400, message, validatorDto != null ? validatorDto.getErrorMessages() : null);
        }

        try {
            FeignFileUploadRequestPayload fileUploadRequest =
                    objectMapper.readValue(fileUploadRequestJson, FeignFileUploadRequestPayload.class);

            List<FileUpload> filesToBeUploaded = new ArrayList<>();

            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                FeignMultipartFileMetadataEntry metadata = fileUploadRequest.getFilesMetadata().get(i);

                RustFsUploadResponse rustResponse = rustFsClient.uploadFile(
                        file,
                        String.valueOf(fileUploadRequest.getOwnerId()),
                        metadata.getFileType());

                String storedFileName = rustResponse.getFileKey();
                String fileUrl = "/files/" + storedFileName;

                FileUpload fileUpload = new FileUpload();
                fileUpload.setOriginalFileName(file.getOriginalFilename());
                fileUpload.setStoredFileName(storedFileName);
                fileUpload.setFileUrl(fileUrl);
                fileUpload.setContentType(file.getContentType());
                fileUpload.setFileSizeInBytes(file.getSize());
                fileUpload.setStorageProvider(StorageProvider.RUST_FS);
                fileUpload.setExpiresAt(metadata.getExpiresAt());
                fileUpload.setEntityStatus(EntityStatus.ACTIVE);
                fileUpload.setOwnerType(OwnerType.valueOf(fileUploadRequest.getOwnerType().toUpperCase()));
                fileUpload.setOwnerId(fileUploadRequest.getOwnerId());
                fileUpload.setFileType(FileType.valueOf(metadata.getFileType()));
                fileUpload.setFileHash(rustResponse.getFileHash());
                fileUpload.setCreatedBy(username != null ? username : "SYSTEM");

                filesToBeUploaded.add(fileUpload);
            }

            List<FileUpload> saved = new ArrayList<>();
            for (FileUpload fu : filesToBeUploaded) {
                saved.add(fileUploadServiceAuditable.save(fu, locale, username));
            }

            String message = messageService.getMessage(I18Code.FILE_UPLOAD_SUCCESS.getCode(), new String[]{}, locale);
            return buildSuccessFromSaved(saved, message, locale);
        } catch (RustFsException e) {
            log.error("RustFS upload failed", e);
            String message = messageService.getMessage(I18Code.FILE_UPLOAD_FAILED.getCode(), new String[]{}, locale);
            return buildErrorResponse(502, message, List.of(e.getMessage()));
        } catch (Exception e) {
            log.error("File upload failed", e);
            String message = messageService.getMessage(I18Code.FILE_UPLOAD_FAILED.getCode(), new String[]{}, locale);
            return buildErrorResponse(500, message, List.of(e.getMessage()));
        }
    }

    @Override
    public FileUploadResponse update(
            List<MultipartFile> files,
            String editFileUploadRequestJson,
            Locale locale,
            String username) {

        ValidatorDto validatorDto = fileUploadServiceValidator.validateUpdate(files, editFileUploadRequestJson, locale);
        if (validatorDto == null || !Boolean.TRUE.equals(validatorDto.getSuccess())) {
            String message = messageService.getMessage(I18Code.FILE_UPLOAD_FAILED.getCode(), new String[]{}, locale);
            return buildErrorResponse(400, message, validatorDto != null ? validatorDto.getErrorMessages() : null);
        }

        try {
            FeignFileUploadRequestPayload editRequest =
                    objectMapper.readValue(editFileUploadRequestJson, FeignFileUploadRequestPayload.class);

            List<FileUpload> saved = new ArrayList<>();

            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                FeignMultipartFileMetadataEntry metadata = editRequest.getFilesMetadata().get(i);

                RustFsUploadResponse rustResponse = rustFsClient.uploadFile(
                        file,
                        String.valueOf(editRequest.getOwnerId()),
                        metadata.getFileType());

                String storedFileName = rustResponse.getFileKey();
                String fileUrl = "/files/" + storedFileName;

                FileUpload fileUpload;
                if (metadata.getId() != null) {
                    Optional<FileUpload> existing =
                            fileUploadRepository.findByIdAndEntityStatusNot(metadata.getId(), EntityStatus.DELETED);
                    fileUpload = existing.orElseGet(FileUpload::new);
                } else {
                    fileUpload = new FileUpload();
                }

                fileUpload.setOriginalFileName(file.getOriginalFilename());
                fileUpload.setStoredFileName(storedFileName);
                fileUpload.setFileUrl(fileUrl);
                fileUpload.setContentType(file.getContentType());
                fileUpload.setFileSizeInBytes(file.getSize());
                fileUpload.setStorageProvider(StorageProvider.RUST_FS);
                fileUpload.setExpiresAt(metadata.getExpiresAt());
                fileUpload.setEntityStatus(EntityStatus.ACTIVE);
                fileUpload.setOwnerType(OwnerType.valueOf(editRequest.getOwnerType().toUpperCase()));
                fileUpload.setOwnerId(editRequest.getOwnerId());
                fileUpload.setFileType(FileType.valueOf(metadata.getFileType()));
                fileUpload.setFileHash(rustResponse.getFileHash());
                fileUpload.setModifiedBy(username != null ? username : "SYSTEM");
                if (fileUpload.getCreatedBy() == null || fileUpload.getCreatedBy().isBlank()) {
                    fileUpload.setCreatedBy(username != null ? username : "SYSTEM");
                }

                saved.add(fileUploadServiceAuditable.save(fileUpload, locale, username));
            }

            String message = messageService.getMessage(I18Code.FILE_UPLOAD_SUCCESS.getCode(), new String[]{}, locale);
            return buildSuccessFromSaved(saved, message, locale);
        } catch (RustFsException e) {
            log.error("RustFS update upload failed", e);
            String message = messageService.getMessage(I18Code.FILE_UPLOAD_FAILED.getCode(), new String[]{}, locale);
            return buildErrorResponse(502, message, List.of(e.getMessage()));
        } catch (Exception e) {
            log.error("File update failed", e);
            String message = messageService.getMessage(I18Code.FILE_UPLOAD_FAILED.getCode(), new String[]{}, locale);
            return buildErrorResponse(500, message, List.of(e.getMessage()));
        }
    }

    @Override
    public FileUploadResponse findById(Long id, Locale locale, String username) {
        ValidatorDto validatorDto = fileUploadServiceValidator.isIdValid(id, locale);
        if (validatorDto == null || !Boolean.TRUE.equals(validatorDto.getSuccess())) {
            String message = messageService.getMessage(I18Code.FILE_UPLOAD_ID_INVALID.getCode(), new String[]{}, locale);
            return buildErrorResponse(400, message, validatorDto != null ? validatorDto.getErrorMessages() : null);
        }

        Optional<FileUpload> optional =
                fileUploadRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (optional.isEmpty()) {
            String message = messageService.getMessage(I18Code.FILE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildErrorResponse(404, message, null);
        }

        FileUpload fileUpload = optional.get();
        FileUploadDto dto = toDtoWithContent(fileUpload, locale);
        String message = messageService.getMessage(I18Code.FILE_UPLOAD_SUCCESS.getCode(), new String[]{}, locale);
        return buildSingleSuccess(200, message, dto);
    }

    @Override
    public FileUploadResponse findByOriginalFileName(String originalFileName, Locale locale, String username) {
        if (originalFileName == null || originalFileName.isBlank()) {
            String message = messageService.getMessage(I18Code.FILE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildErrorResponse(400, message, null);
        }

        Optional<FileUpload> optional =
                fileUploadRepository.findByOriginalFileNameAndEntityStatusNot(originalFileName, EntityStatus.DELETED);
        if (optional.isEmpty()) {
            String message = messageService.getMessage(I18Code.FILE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildErrorResponse(404, message, null);
        }

        FileUpload fileUpload = optional.get();
        FileUploadDto dto = toDtoWithContent(fileUpload, locale);
        String message = messageService.getMessage(I18Code.FILE_UPLOAD_SUCCESS.getCode(), new String[]{}, locale);
        return buildSingleSuccess(200, message, dto);
    }

    @Override
    public FileUploadResponse findByOwnerTypeAndId(OwnerType ownerType, Long ownerId, Locale locale, String username) {
        if (ownerType == null || ownerId == null) {
            String message = messageService.getMessage(I18Code.FILE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildErrorResponse(400, message, null);
        }

        List<FileUpload> filesRetrieved =
                fileUploadRepository.findByOwnerTypeAndOwnerIdAndEntityStatusNot(ownerType, ownerId, EntityStatus.DELETED);

        List<FileUploadDto> fileUploadDtoList = filesRetrieved.stream().map(file -> {
            FileUploadDto dto = modelMapper.map(file, FileUploadDto.class);
            String fileUrl = "/files/" + file.getStoredFileName();
            dto.setFileUrl(fileUrl);
            dto.setUpdatedAt(file.getModifiedAt());

            try {
                byte[] fileBytes = readFileBytes(file);
                String base64FileContent = Base64.getEncoder().encodeToString(fileBytes);
                dto.setFileContent(base64FileContent);
            } catch (Exception e) {
                dto.setFileContent(null);
            }

            return dto;
        }).toList();

        FileUploadResponse response = new FileUploadResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage(messageService.getMessage(I18Code.FILE_UPLOAD_SUCCESS.getCode(), new String[]{}, locale));
        response.setFileUploadDtoList(fileUploadDtoList);
        return response;
    }

    @Override
    public FileUploadResponse delete(Long id, Locale locale, String username) {
        ValidatorDto validatorDto = fileUploadServiceValidator.isIdValid(id, locale);
        if (validatorDto == null || !Boolean.TRUE.equals(validatorDto.getSuccess())) {
            String message = messageService.getMessage(I18Code.FILE_UPLOAD_ID_INVALID.getCode(), new String[]{}, locale);
            return buildErrorResponse(400, message, validatorDto != null ? validatorDto.getErrorMessages() : null);
        }

        Optional<FileUpload> optional =
                fileUploadRepository.findByIdAndEntityStatusNot(id, EntityStatus.DELETED);
        if (optional.isEmpty()) {
            String message = messageService.getMessage(I18Code.FILE_NOT_FOUND.getCode(), new String[]{}, locale);
            return buildErrorResponse(404, message, null);
        }

        FileUpload fileToBeDeleted = optional.get();
        FileUpload fileDeleted = fileUploadServiceAuditable.delete(fileToBeDeleted, locale, username);

        if (fileToBeDeleted.getStorageProvider() == StorageProvider.RUST_FS) {
            try {
                rustFsClient.deleteFile(fileToBeDeleted.getStoredFileName());
            } catch (RustFsException e) {
                log.warn("Failed to soft-delete file in RustFS: {}. Error: {}",
                        fileToBeDeleted.getStoredFileName(), e.getMessage());
            }
        }

        String message = messageService.getMessage(I18Code.FILE_DELETE_SUCCESS.getCode(), new String[]{}, locale);
        FileUploadResponse response = new FileUploadResponse();
        response.setStatusCode(200);
        response.setSuccess(true);
        response.setMessage(message);
        response.setFileUploadDto(modelMapper.map(fileDeleted, FileUploadDto.class));
        return response;
    }

    /**
     * Reads file bytes using the appropriate storage backend based on the record's storageProvider.
     * LOCAL records (pre-migration) read from local disk.
     * RUST_FS records read from the Rust File Service.
     */
    private byte[] readFileBytes(FileUpload fileUpload) {
        if (fileUpload.getStorageProvider() == StorageProvider.LOCAL) {
            return localFileStorageService.readFile(fileUpload.getStoredFileName());
        }
        RustFsDownloadResponse rustResponse = rustFsClient.downloadFile(fileUpload.getStoredFileName());
        return rustResponse.getFileBytes();
    }

    private FileUploadDto toDtoWithContent(FileUpload fileUpload, Locale locale) {
        FileUploadDto dto = modelMapper.map(fileUpload, FileUploadDto.class);
        String fileUrl = "/files/" + fileUpload.getStoredFileName();
        dto.setFileUrl(fileUrl);
        dto.setUpdatedAt(fileUpload.getModifiedAt());
        try {
            byte[] fileBytes = readFileBytes(fileUpload);
            dto.setFileContent(Base64.getEncoder().encodeToString(fileBytes));
        } catch (Exception e) {
            log.warn("Could not read file content for id {}: {}", fileUpload.getId(), e.getMessage());
            dto.setFileContent(null);
        }
        return dto;
    }

    private FileUploadResponse buildSuccessFromSaved(List<FileUpload> saved, String message, Locale locale) {
        List<FileUploadDto> dtos = saved.stream().map(fu -> toDtoWithContent(fu, locale)).toList();
        FileUploadResponse response = new FileUploadResponse();
        response.setStatusCode(201);
        response.setSuccess(true);
        response.setMessage(message);
        response.setFileUploadDtoList(dtos);
        if (dtos.size() == 1) {
            response.setFileUploadDto(dtos.get(0));
        }
        return response;
    }

    private FileUploadResponse buildSingleSuccess(int status, String message, FileUploadDto dto) {
        FileUploadResponse response = new FileUploadResponse();
        response.setStatusCode(status);
        response.setSuccess(true);
        response.setMessage(message);
        response.setFileUploadDto(dto);
        response.setFileUploadDtoList(List.of(dto));
        return response;
    }

    private FileUploadResponse buildErrorResponse(int status, String message, List<String> errors) {
        FileUploadResponse response = new FileUploadResponse();
        response.setStatusCode(status);
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessages(errors);
        return response;
    }

    @SuppressWarnings("unused")
    private String computeFileHash(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(file.getBytes());
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
