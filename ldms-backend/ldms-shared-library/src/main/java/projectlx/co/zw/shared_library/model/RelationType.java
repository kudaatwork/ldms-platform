package projectlx.co.zw.shared_library.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RelationType {
    EMPLOYER("EMPLOYER"),
    REPRESENTATIVE("REPRESENTATIVE"),
    PARTNER("PARTNER");

    private final String relationType;
}
