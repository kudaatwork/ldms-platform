package projectlx.co.zw.locationsmanagementservice.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.SuburbServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.locationsmanagementservice.repository.SuburbRepository;
import java.util.Locale;

@RequiredArgsConstructor
public class SuburbServiceAuditableImpl implements SuburbServiceAuditable {

    private final SuburbRepository suburbRepository;

    @Override
    public Suburb create(Suburb suburb, Locale locale, String username) {
        return suburbRepository.save(suburb);
    }

    @Override
    public Suburb update(Suburb suburb, Locale locale, String username) {
        return suburbRepository.save(suburb);
    }

    @Override
    public Suburb delete(Suburb suburb, Locale locale, String username) {
        return suburbRepository.save(suburb);
    }
}