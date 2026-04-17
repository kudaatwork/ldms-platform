package projectlx.co.zw.shared_library.utils.rustfs;

import org.springframework.web.multipart.MultipartFile;

/**
 * Client for the Rust File Storage Service (ldms-file-service, port 8200).
 * Handles direct byte operations: upload, download, delete, metadata.
 *
 * <p>All Java microservices use this client for file byte operations instead
 * of proxying through the Java file upload service.
 *
 * <p>Security: Attaches X-Internal-Token header on all requests.
 * Discovery: Resolves service URL via rustfs.base-url config property.
 */
public interface RustFsClient {

    /**
     * Upload file bytes directly to Rust FS.
     *
     * @param file            the multipart file to upload
     * @param organizationId  the owning organization's ID (becomes part of the storage path)
     * @param referenceType   the file category (e.g., LOGO, CUSTOMS_DECLARATION — must match FileType enum values)
     * @return RustFsUploadResponse containing fileKey, contentType, sizeBytes, fileHash, storedAt
     * @throws RustFsException if the upload fails
     */
    RustFsUploadResponse uploadFile(MultipartFile file, String organizationId, String referenceType);

    /**
     * Download file bytes directly from Rust FS.
     *
     * @param fileKey the full storage key (e.g., "123/LOGO/uuid.png")
     * @return RustFsDownloadResponse containing byte array and contentType
     * @throws RustFsException if the download fails or file not found
     */
    RustFsDownloadResponse downloadFile(String fileKey);

    /**
     * Soft-delete a file in Rust FS. Creates a .deleted marker; does not remove bytes.
     *
     * @param fileKey the full storage key
     * @return RustFsDeleteResponse containing fileKey and deletedAt
     * @throws RustFsException if the delete fails
     */
    RustFsDeleteResponse deleteFile(String fileKey);

    /**
     * Get file metadata from Rust FS without downloading bytes.
     *
     * @param fileKey the full storage key
     * @return RustFsMetadataResponse containing file attributes
     * @throws RustFsException if the metadata fetch fails or file not found
     */
    RustFsMetadataResponse getMetadata(String fileKey);
}
