package projectlx.co.zw.shared_library.utils.i18n.api;

import java.util.Locale;

public interface MessageService {

    String getMessage(String code, String[] args, Locale locale);
}
