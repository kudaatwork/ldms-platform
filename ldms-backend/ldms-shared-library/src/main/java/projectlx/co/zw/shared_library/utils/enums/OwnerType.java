package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum OwnerType {
    USER("USER"),
    ORGANIZATION("ORGANIZATION");

    private final String ownerType;
}
