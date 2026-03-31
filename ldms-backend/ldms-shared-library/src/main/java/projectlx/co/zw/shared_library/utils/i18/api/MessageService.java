package projectlx.co.zw.shared_library.utils.i18.api;

import java.util.Locale;

public interface MessageService {
    String getMessage(String propertyName, String[] placeholders, Locale locale);
    String getMessage(String propertyName, Locale locale);
    String getMessage(String propertyName, String[] placeholders, String defaultMessage, Locale locale);
}
