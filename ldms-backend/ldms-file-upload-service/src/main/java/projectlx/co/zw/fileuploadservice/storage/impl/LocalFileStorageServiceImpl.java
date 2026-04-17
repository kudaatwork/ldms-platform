package projectlx.co.zw.fileuploadservice.storage.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import projectlx.co.zw.fileuploadservice.storage.FileStorageService;
import projectlx.co.zw.fileuploadservice.utils.config.FileUploadProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final FileUploadProperties fileUploadProperties;

    @Override
    public String storeFile(MultipartFile file) {
        try {
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
            String storedFileName = System.currentTimeMillis() + "_" + System.nanoTime() + "_" + original;
            Path dir = Paths.get(fileUploadProperties.getLocation());
            Files.createDirectories(dir);
            Path target = dir.resolve(storedFileName);
            Files.write(target, file.getBytes());
            return storedFileName;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file locally", e);
        }
    }

    @Override
    public List<String> storeFiles(List<MultipartFile> files) {
        List<String> names = new ArrayList<>();
        for (MultipartFile file : files) {
            names.add(storeFile(file));
        }
        return names;
    }

    @Override
    public byte[] readFile(String storedFileName) {
        try {
            Path path = Paths.get(fileUploadProperties.getLocation()).resolve(storedFileName);
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file: " + storedFileName, e);
        }
    }
}
