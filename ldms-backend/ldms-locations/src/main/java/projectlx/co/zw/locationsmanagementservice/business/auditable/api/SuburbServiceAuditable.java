package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import java.util.Locale;

public interface SuburbServiceAuditable {
    Suburb create(Suburb suburb, Locale locale, String username);
    Suburb update(Suburb suburb, Locale locale, String username);
    Suburb delete(Suburb suburb, Locale locale, String username);
}
