package projectlx.co.zw.locationsmanagementservice.business.auditable.api;

import projectlx.co.zw.locationsmanagementservice.model.Address;
import java.util.Locale;

public interface AddressServiceAuditable {
    Address create(Address address, Locale locale, String username);
    Address update(Address address, Locale locale, String username);
    Address delete(Address address, Locale locale);
}