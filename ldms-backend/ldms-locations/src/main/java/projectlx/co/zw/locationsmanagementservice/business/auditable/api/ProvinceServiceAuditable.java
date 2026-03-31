package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import projectlx.co.zw.locationsmanagementservice.model.Province;
import java.util.Locale;

public interface ProvinceServiceAuditable {
    Province create(Province province, Locale locale, String username);
    Province update(Province province, Locale locale, String username);
    Province delete(Province province, Locale locale);
}