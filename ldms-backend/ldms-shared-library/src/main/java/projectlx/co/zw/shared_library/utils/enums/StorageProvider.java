package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum StorageProvider {
    LOCAL("LOCAL"),
    AZURE("AZURE"),
    AWS("AWS");
    private final String storageProvider;
}
