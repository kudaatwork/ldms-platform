package projectlx.co.zw.fileuploadservice.service.rest.support;

import org.springframework.http.ResponseEntity;
import projectlx.co.zw.shared_library.utils.enums.OwnerType;
import projectlx.co.zw.shared_library.utils.responses.FileUploadResponse;

import java.util.List;
import java.util.Locale;

/**
 * Normalizes {@code ownerType} query values (e.g. strips {@code OwnerType.} prefixes from clients)
 * and builds consistent 400 responses when parsing fails.
 */
public final class FileUploadOwnerTypeParam {

    private FileUploadOwnerTypeParam() {}

    public static OwnerType parse(String ownerType) {
        if (ownerType == null || ownerType.isBlank()) {
            throw new IllegalArgumentException("ownerType must not be blank");
        }
        String t = ownerType.trim();
        int dot = t.lastIndexOf('.');
        if (dot >= 0) {
            t = t.substring(dot + 1);
        }
        t = t.toUpperCase(Locale.ROOT);
        return OwnerType.valueOf(t);
    }

    public static ResponseEntity<FileUploadResponse> invalidOwnerTypeResponse(String detail) {
        FileUploadResponse r = new FileUploadResponse();
        r.setStatusCode(400);
        r.setSuccess(false);
        r.setMessage("Invalid ownerType. Expected USER or ORGANIZATION.");
        r.setErrorMessages(List.of(detail != null ? detail : "Invalid ownerType"));
        return ResponseEntity.badRequest().body(r);
    }
}
