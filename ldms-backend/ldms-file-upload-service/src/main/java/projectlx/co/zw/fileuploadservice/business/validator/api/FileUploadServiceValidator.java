package projectlx.co.zw.fileuploadservice.business.validator.api;

import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.List;
import java.util.Locale;

public interface FileUploadServiceValidator {

    ValidatorDto validateUpload(List<MultipartFile> files, String fileUploadRequestJson, Locale locale);

    ValidatorDto validateUpdate(List<MultipartFile> files, String editFileUploadRequestJson, Locale locale);

    ValidatorDto isIdValid(Long id, Locale locale);
}
