package projectlx.fuel.expenses.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.fuel.expenses.business.auditable.api.OperationalFundRequestServiceAuditable;
import projectlx.fuel.expenses.model.OperationalFundRequest;
import projectlx.fuel.expenses.repository.OperationalFundRequestRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class OperationalFundRequestServiceAuditableImpl implements OperationalFundRequestServiceAuditable {

    private final OperationalFundRequestRepository operationalFundRequestRepository;

    @Override
    public OperationalFundRequest create(OperationalFundRequest request, Locale locale, String username) {
        return operationalFundRequestRepository.save(request);
    }

    @Override
    public OperationalFundRequest update(OperationalFundRequest request, Locale locale, String username) {
        return operationalFundRequestRepository.save(request);
    }
}
