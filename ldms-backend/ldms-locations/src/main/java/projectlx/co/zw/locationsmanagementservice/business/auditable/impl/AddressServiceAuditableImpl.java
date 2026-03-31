package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.AddressServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.Address;
import projectlx.co.zw.locationsmanagementservice.repository.AddressRepository;
import java.util.Locale;

@RequiredArgsConstructor
public class AddressServiceAuditableImpl implements AddressServiceAuditable {

    private final AddressRepository addressRepository;

    @Override
    public Address create(Address address, Locale locale, String username) {
        return addressRepository.save(address);
    }

    @Override
    public Address update(Address address, Locale locale, String username) {
        return addressRepository.save(address);
    }

    @Override
    public Address delete(Address address, Locale locale) {
        return addressRepository.save(address);
    }
}