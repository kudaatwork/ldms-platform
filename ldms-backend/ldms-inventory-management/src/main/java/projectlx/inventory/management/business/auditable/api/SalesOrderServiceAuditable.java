package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.SalesOrder;

import java.util.Locale;

public interface SalesOrderServiceAuditable {
    SalesOrder create(SalesOrder salesOrder, Locale locale, String username);
    SalesOrder update(SalesOrder salesOrder, Locale locale, String username);
    SalesOrder delete(SalesOrder salesOrder, Locale locale);
}
