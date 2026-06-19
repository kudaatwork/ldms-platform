package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.inventory.management.business.auditable.api.CrossDockDispatchServiceAuditable;
import projectlx.inventory.management.model.CrossDockDispatch;
import projectlx.inventory.management.repository.CrossDockDispatchRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class CrossDockDispatchServiceAuditableImpl implements CrossDockDispatchServiceAuditable {

    private final CrossDockDispatchRepository crossDockDispatchRepository;

    @Override
    public CrossDockDispatch create(CrossDockDispatch dispatch, Locale locale, String username) {
        return crossDockDispatchRepository.save(dispatch);
    }

    @Override
    public CrossDockDispatch update(CrossDockDispatch dispatch, Locale locale, String username) {
        return crossDockDispatchRepository.save(dispatch);
    }

    @Override
    public CrossDockDispatch delete(CrossDockDispatch dispatch, Locale locale) {
        return crossDockDispatchRepository.save(dispatch);
    }
}
