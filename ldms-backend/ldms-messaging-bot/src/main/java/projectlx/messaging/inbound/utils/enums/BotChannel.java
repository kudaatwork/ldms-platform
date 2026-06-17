package projectlx.messaging.inbound.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BotChannel {
    WHATSAPP("WhatsApp"),
    WEB("Web"),
    SMS("SMS");

    private final String label;
}
