package projectlx.messaging.inbound.service.processor.api;

import projectlx.messaging.inbound.utils.requests.CreateBotFaqRequest;
import projectlx.messaging.inbound.utils.requests.EditBotFaqRequest;
import projectlx.messaging.inbound.utils.responses.BotFaqResponse;

import java.util.Locale;

public interface BotFaqServiceProcessor {

    BotFaqResponse listAll(Locale locale);

    BotFaqResponse findById(Long id, Locale locale);

    BotFaqResponse create(CreateBotFaqRequest request, Locale locale, String username);

    BotFaqResponse update(Long id, EditBotFaqRequest request, Locale locale, String username);

    BotFaqResponse delete(Long id, Locale locale, String username);
}
