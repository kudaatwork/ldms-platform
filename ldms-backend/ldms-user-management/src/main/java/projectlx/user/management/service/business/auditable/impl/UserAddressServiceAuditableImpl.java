package projectlx.user.management.service.business.auditable.impl;

import projectlx.user.management.service.business.auditable.api.UserAddressServiceAuditable;
import projectlx.user.management.service.model.Address;
import projectlx.user.management.service.repository.UserAddressRepository;
import lombok.RequiredArgsConstructor;
import java.util.Locale;

@RequiredArgsConstructor
public class UserAddressServiceAuditableImpl implements UserAddressServiceAuditable {

    private final UserAddressRepository userAddressRepository;

    @Override
    public Address create(Address address, Locale locale, String username) {
        return userAddressRepository.save(address);
    }

    @Override
    public Address update(Address address, Locale locale, String username) {
        return userAddressRepository.save(address);
    }

    @Override
    public Address delete(Address address, Locale locale) {
        return userAddressRepository.save(address);
    }
}
