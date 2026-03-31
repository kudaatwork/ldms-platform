package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import projectlx.co.zw.locationsmanagementservice.model.Country;
import java.util.Locale;

public interface CountryServiceAuditable {
    Country create(Country country, Locale locale, String username);
    Country update(Country country, Locale locale, String username);
    Country delete(Country country, Locale locale, String username);
}