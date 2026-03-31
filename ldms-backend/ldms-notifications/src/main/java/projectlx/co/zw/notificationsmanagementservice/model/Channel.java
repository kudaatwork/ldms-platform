package projectlx.co.zw.notificationsmanagementservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Channel {

    EMAIL("EMAIL"),
    SMS("SMS"),
    WHATSAPP("WHATSAPP"),
    IN_APP("IN_APP"),
    SLACK("SLACK"),
    TEAMS("TEAMS");

    private final String channel;
}
