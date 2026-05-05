package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import projectlx.co.zw.locationsmanagementservice.model.City;

import java.util.Locale;

public interface CityServiceAuditable {

    City create(City city, Locale locale, String username);

    City update(City city, Locale locale, String username);

    City delete(City city, Locale locale);
}
