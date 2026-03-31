package projectlx.user.management.service.business.auditable.api;

import projectlx.user.management.service.model.Address;

import java.util.Locale;

public interface UserAddressServiceAuditable {
    Address create(Address address, Locale locale, String username);
    Address update(Address address, Locale locale, String username);
    Address delete(Address address, Locale locale);
}
