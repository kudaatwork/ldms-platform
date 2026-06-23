package projectlx.messaging.inbound.repository.projection;

import java.time.LocalDate;

public interface BotMessageDailyCountProjection {

    LocalDate getDay();

    Long getUserMessages();

    Long getBotMessages();
}
