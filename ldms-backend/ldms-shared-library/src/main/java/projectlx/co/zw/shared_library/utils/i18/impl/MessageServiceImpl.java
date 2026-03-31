package projectlx.co.zw.shared_library.utils.i18.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageSource messageSource;

    @Override
    public String getMessage(String propertyName, String[] placeholders, Locale locale) {
        return messageSource.getMessage(propertyName, placeholders, locale);
    }

    @Override
    public String getMessage(String propertyName, Locale locale) {
        return messageSource.getMessage(propertyName, new String[] {}, locale);
    }

    @Override
    public String getMessage(String propertyName, String[] placeholders, String defaultMessage, Locale locale) {
        return messageSource.getMessage(propertyName, placeholders, defaultMessage, locale);
    }
}
