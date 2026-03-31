package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Gender {
    MALE("MALE"),      // Male
    FEMALE("FEMALE"),    // Female
    NON_BINARY("NON_BINARY"); // Non-binary

    private final String gender;
}
