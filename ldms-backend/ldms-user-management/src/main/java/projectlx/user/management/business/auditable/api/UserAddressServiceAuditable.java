package projectlx.user.management.business.auditable.api;

import projectlx.user.management.model.Address;

import java.util.Locale;

public interface UserAddressServiceAuditable {
    Address create(Address address, Locale locale, String username);
    Address update(Address address, Locale locale, String username);
    Address delete(Address address, Locale locale);
}
