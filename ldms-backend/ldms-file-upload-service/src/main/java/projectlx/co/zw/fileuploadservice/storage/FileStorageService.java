package projectlx.co.zw.fileuploadservice.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    String storeFile(MultipartFile file);

    List<String> storeFiles(List<MultipartFile> files);

    byte[] readFile(String storedFileName);
}
