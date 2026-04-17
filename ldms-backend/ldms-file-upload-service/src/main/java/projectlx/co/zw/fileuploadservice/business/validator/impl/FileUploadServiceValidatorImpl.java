package projectlx.co.zw.fileuploadservice.business.validator.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.fileuploadservice.business.validator.api.FileUploadServiceValidator;
import projectlx.co.zw.fileuploadservice.utils.enums.I18Code;
import projectlx.co.zw.fileuploadservice.utils.json.FeignFileUploadRequestPayload;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class FileUploadServiceValidatorImpl implements FileUploadServiceValidator {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @Override
    public ValidatorDto validateUpload(List<MultipartFile> files, String fileUploadRequestJson, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (CollectionUtils.isEmpty(files)) {
            errors.add(messageService.getMessage(I18Code.FILE_UPLOAD_FILES_REQUIRED.getCode(), new String[]{}, locale));
        }
        if (fileUploadRequestJson == null || fileUploadRequestJson.isBlank()) {
            errors.add(messageService.getMessage(I18Code.FILE_UPLOAD_METADATA_REQUIRED.getCode(), new String[]{}, locale));
        } else {
            try {
                FeignFileUploadRequestPayload p = objectMapper.readValue(fileUploadRequestJson, FeignFileUploadRequestPayload.class);
                if (p.getOwnerType() == null || p.getOwnerId() == null || CollectionUtils.isEmpty(p.getFilesMetadata())) {
                    errors.add(messageService.getMessage(I18Code.FILE_UPLOAD_METADATA_INVALID.getCode(), new String[]{}, locale));
                } else if (!CollectionUtils.isEmpty(files) && p.getFilesMetadata().size() != files.size()) {
                    errors.add(messageService.getMessage(I18Code.FILE_UPLOAD_COUNT_MISMATCH.getCode(), new String[]{}, locale));
                }
            } catch (Exception e) {
                errors.add(messageService.getMessage(I18Code.FILE_UPLOAD_METADATA_INVALID.getCode(), new String[]{}, locale));
            }
        }
        return errors.isEmpty()
                ? new ValidatorDto(true, null, null)
                : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto validateUpdate(List<MultipartFile> files, String editFileUploadRequestJson, Locale locale) {
        return validateUpload(files, editFileUploadRequestJson, locale);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        if (id == null || id < 1) {
            return new ValidatorDto(false, null, List.of(
                    messageService.getMessage(I18Code.FILE_UPLOAD_ID_INVALID.getCode(), new String[]{}, locale)));
        }
        return new ValidatorDto(true, null, null);
    }
}
