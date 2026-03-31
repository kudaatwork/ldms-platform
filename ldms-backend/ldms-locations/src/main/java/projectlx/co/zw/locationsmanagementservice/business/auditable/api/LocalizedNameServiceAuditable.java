package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import projectlx.co.zw.locationsmanagementservice.model.LocalizedName;
import java.util.Locale;

public interface LocalizedNameServiceAuditable {
    LocalizedName create(LocalizedName localizedName, Locale locale, String username);
    LocalizedName update(LocalizedName localizedName, Locale locale, String username);
    LocalizedName delete(LocalizedName localizedName, Locale locale);
}