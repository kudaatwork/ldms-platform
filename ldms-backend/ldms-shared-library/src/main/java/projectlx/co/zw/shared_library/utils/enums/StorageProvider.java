package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum StorageProvider {
    LOCAL("LOCAL"),
    AZURE("AZURE"),
    AWS("AWS"),
    /** Files stored via the Rust file service (ldms-file-service, port 8200). */
    RUST_FS("RUST_FS");
    private final String storageProvider;
}
