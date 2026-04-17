package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import java.util.Locale;
import projectlx.co.zw.locationsmanagementservice.model.LocationNode;

public interface LocationNodeServiceAuditable {
    LocationNode create(LocationNode locationNode, Locale locale, String username);

    LocationNode update(LocationNode locationNode, Locale locale, String username);

    LocationNode delete(LocationNode locationNode, Locale locale, String username);
}
