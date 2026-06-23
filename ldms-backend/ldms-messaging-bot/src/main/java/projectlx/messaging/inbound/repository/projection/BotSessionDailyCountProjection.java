package projectlx.messaging.inbound.repository.projection;

import java.time.LocalDate;

public interface BotSessionDailyCountProjection {

    LocalDate getDay();

    Long getCount();
}
