package projectlx.co.zw.organizationmanagement.clients;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * Placeholder Feign client for file-upload-service (metadata / Rust FS integration).
 */
@FeignClient(name = "file-upload-service")
public interface FileUploadServiceClient {
}
