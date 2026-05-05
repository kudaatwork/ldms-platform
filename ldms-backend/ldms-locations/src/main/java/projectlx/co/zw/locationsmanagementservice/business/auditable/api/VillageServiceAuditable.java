package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import projectlx.co.zw.locationsmanagementservice.model.Village;

import java.util.Locale;

public interface VillageServiceAuditable {

    Village create(Village village, Locale locale, String username);

    Village update(Village village, Locale locale, String username);

    Village delete(Village village, Locale locale);
}
