package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel;
import java.util.Locale;

public interface AdministrativeLevelServiceAuditable {
    AdministrativeLevel create(AdministrativeLevel administrativeLevel, Locale locale, String username);
    AdministrativeLevel update(AdministrativeLevel administrativeLevel, Locale locale, String username);
    AdministrativeLevel delete(AdministrativeLevel administrativeLevel, Locale locale);
}