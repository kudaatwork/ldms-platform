package projectlx.messaging.inbound.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BotMessageRole {
    USER("user"),
    BOT("bot"),
    SYSTEM("system");

    private final String wireValue;
}
