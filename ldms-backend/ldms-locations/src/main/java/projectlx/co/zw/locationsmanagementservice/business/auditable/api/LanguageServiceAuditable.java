package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import projectlx.co.zw.locationsmanagementservice.model.Language;
import java.util.Locale;

public interface LanguageServiceAuditable {
    Language create(Language language, Locale locale, String username);
    Language update(Language language, Locale locale, String username);
    Language delete(Language language, Locale locale);
}