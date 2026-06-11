package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.SalesOrderLine;

import java.util.Locale;

public interface SalesOrderLineServiceAuditable {
    SalesOrderLine create(SalesOrderLine salesOrderLine, Locale locale, String username);
    SalesOrderLine update(SalesOrderLine salesOrderLine, Locale locale, String username);
    SalesOrderLine delete(SalesOrderLine salesOrderLine, Locale locale);
}
