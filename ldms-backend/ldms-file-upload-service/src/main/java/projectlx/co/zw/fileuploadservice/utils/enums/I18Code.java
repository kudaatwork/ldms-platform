package projectlx.co.zw.fileuploadservice.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum I18Code {
    FILE_UPLOAD_FILES_REQUIRED("file.upload.files.required"),
    FILE_UPLOAD_METADATA_REQUIRED("file.upload.metadata.required"),
    FILE_UPLOAD_METADATA_INVALID("file.upload.metadata.invalid"),
    FILE_UPLOAD_COUNT_MISMATCH("file.upload.count.mismatch"),
    FILE_UPLOAD_ID_INVALID("file.upload.id.invalid"),
    FILE_UPLOAD_SUCCESS("file.upload.success"),
    FILE_UPLOAD_FAILED("file.upload.failed"),
    FILE_NOT_FOUND("file.upload.not.found"),
    FILE_DELETE_SUCCESS("file.delete.success");

    private final String code;
}
