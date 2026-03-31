package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import projectlx.co.zw.locationsmanagementservice.model.District;
import java.util.Locale;

public interface DistrictServiceAuditable {
    District create(District district, Locale locale, String username);
    District update(District district, Locale locale, String username);
    District delete(District district, Locale locale);
}