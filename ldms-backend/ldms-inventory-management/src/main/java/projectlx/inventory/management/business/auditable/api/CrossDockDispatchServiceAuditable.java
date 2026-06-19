package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.CrossDockDispatch;

import java.util.Locale;

public interface CrossDockDispatchServiceAuditable {
    CrossDockDispatch create(CrossDockDispatch dispatch, Locale locale, String username);
    CrossDockDispatch update(CrossDockDispatch dispatch, Locale locale, String username);
    CrossDockDispatch delete(CrossDockDispatch dispatch, Locale locale);
}
