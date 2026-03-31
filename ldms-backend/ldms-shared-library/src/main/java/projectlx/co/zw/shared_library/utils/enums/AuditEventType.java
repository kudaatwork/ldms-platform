package projectlx.co.zw.shared_library.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AuditEventType {
    WEB_REQUEST("WEB_REQUEST"),
    SERVICE_METHOD("SERVICE_METHOD"),
    FEIGN_CALL("FEIGN_CALL"),
    EXCEPTION("EXCEPTION"),;

    private final String auditEventType;
}
