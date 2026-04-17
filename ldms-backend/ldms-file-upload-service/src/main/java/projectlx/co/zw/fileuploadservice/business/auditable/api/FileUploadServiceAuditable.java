package projectlx.co.zw.fileuploadservice.business.auditable.api;

import projectlx.co.zw.fileuploadservice.model.FileUpload;

import java.util.Locale;

public interface FileUploadServiceAuditable {

    FileUpload save(FileUpload fileUpload, Locale locale, String username);

    FileUpload delete(FileUpload fileUpload, Locale locale, String username);
}
