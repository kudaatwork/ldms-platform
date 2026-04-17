package projectlx.co.zw.shared_library.utils.rustfs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
public class RustFsClientImpl implements RustFsClient {

    private final RestTemplate restTemplate;
    private final String rustFsBaseUrl;
    private final String internalToken;

    public RustFsClientImpl(RestTemplate restTemplate, String rustFsBaseUrl, String internalToken) {
        this.restTemplate = restTemplate;
        this.rustFsBaseUrl = rustFsBaseUrl == null ? "" : rustFsBaseUrl.replaceAll("/+$", "");
        this.internalToken = internalToken == null ? "" : internalToken;
    }

    @Override
    public RustFsUploadResponse uploadFile(MultipartFile file, String organizationId, String referenceType) {
        log.info("Uploading file to RustFS: orgId={}, refType={}, fileName={}, size={}",
                organizationId, referenceType, file.getOriginalFilename(), file.getSize());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-Internal-Token", internalToken);

            byte[] fileBytes = file.getBytes();
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);
            body.add("organizationId", organizationId);
            body.add("referenceType", referenceType);
            if (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()) {
                body.add("originalFileName", file.getOriginalFilename());
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<RustFsUploadResponse> response = restTemplate.exchange(
                    rustFsBaseUrl + "/files/upload",
                    HttpMethod.POST,
                    requestEntity,
                    RustFsUploadResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("File uploaded to RustFS successfully: fileKey={}", response.getBody().getFileKey());
                return response.getBody();
            }

            throw new RustFsException(response.getStatusCode().value(), "UploadFailed",
                    "RustFS returned non-success status: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("RustFS upload client error HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RustFsException(e.getStatusCode().value(), "UploadFailed", e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("RustFS upload server error HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RustFsException(e.getStatusCode().value(), "UploadFailed", e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("RustFS upload connection failed: {}", e.getMessage());
            throw new RustFsException(503, "ServiceUnavailable",
                    "Rust File Service is unavailable: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Failed to read file bytes for RustFS upload: {}", file.getOriginalFilename(), e);
            throw new RustFsException(500, "IOError", "Failed to read file bytes: " + e.getMessage(), e);
        } catch (RustFsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error uploading to RustFS: {}", e.getMessage(), e);
            throw new RustFsException(500, "InternalError", "RustFS upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public RustFsDownloadResponse downloadFile(String fileKey) {
        log.debug("Downloading file from RustFS: fileKey={}", fileKey);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", internalToken);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    rustFsBaseUrl + "/files/" + fileKey,
                    HttpMethod.GET,
                    requestEntity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                RustFsDownloadResponse downloadResponse = new RustFsDownloadResponse();
                downloadResponse.setFileBytes(response.getBody());
                downloadResponse.setContentType(
                        response.getHeaders().getContentType() != null
                                ? response.getHeaders().getContentType().toString()
                                : "application/octet-stream"
                );
                downloadResponse.setFileKey(fileKey);
                return downloadResponse;
            }

            throw new RustFsException(response.getStatusCode().value(), "DownloadFailed",
                    "RustFS returned non-success status: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("RustFS download client error HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RustFsException(e.getStatusCode().value(), "DownloadFailed", e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("RustFS download server error HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RustFsException(e.getStatusCode().value(), "DownloadFailed", e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("RustFS download connection failed: {}", e.getMessage());
            throw new RustFsException(503, "ServiceUnavailable",
                    "Rust File Service is unavailable: " + e.getMessage(), e);
        } catch (RustFsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error downloading from RustFS: {}", e.getMessage(), e);
            throw new RustFsException(500, "InternalError", "RustFS download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public RustFsDeleteResponse deleteFile(String fileKey) {
        log.info("Soft-deleting file in RustFS: fileKey={}", fileKey);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", internalToken);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<RustFsDeleteResponse> response = restTemplate.exchange(
                    rustFsBaseUrl + "/files/" + fileKey,
                    HttpMethod.DELETE,
                    requestEntity,
                    RustFsDeleteResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("File soft-deleted in RustFS: fileKey={}", fileKey);
                return response.getBody();
            }

            throw new RustFsException(response.getStatusCode().value(), "DeleteFailed",
                    "RustFS returned non-success status: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("RustFS delete client error HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RustFsException(e.getStatusCode().value(), "DeleteFailed", e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("RustFS delete server error HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RustFsException(e.getStatusCode().value(), "DeleteFailed", e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("RustFS delete connection failed: {}", e.getMessage());
            throw new RustFsException(503, "ServiceUnavailable",
                    "Rust File Service is unavailable: " + e.getMessage(), e);
        } catch (RustFsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting in RustFS: {}", e.getMessage(), e);
            throw new RustFsException(500, "InternalError", "RustFS delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public RustFsMetadataResponse getMetadata(String fileKey) {
        log.debug("Fetching metadata from RustFS: fileKey={}", fileKey);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", internalToken);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<RustFsMetadataResponse> response = restTemplate.exchange(
                    rustFsBaseUrl + "/files/metadata/" + fileKey,
                    HttpMethod.GET,
                    requestEntity,
                    RustFsMetadataResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }

            throw new RustFsException(response.getStatusCode().value(), "MetadataFailed",
                    "RustFS returned non-success status: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            log.error("RustFS metadata client error HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RustFsException(e.getStatusCode().value(), "MetadataFailed", e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("RustFS metadata server error HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RustFsException(e.getStatusCode().value(), "MetadataFailed", e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.error("RustFS metadata connection failed: {}", e.getMessage());
            throw new RustFsException(503, "ServiceUnavailable",
                    "Rust File Service is unavailable: " + e.getMessage(), e);
        } catch (RustFsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching RustFS metadata: {}", e.getMessage(), e);
            throw new RustFsException(500, "InternalError", "RustFS metadata failed: " + e.getMessage(), e);
        }
    }
}
