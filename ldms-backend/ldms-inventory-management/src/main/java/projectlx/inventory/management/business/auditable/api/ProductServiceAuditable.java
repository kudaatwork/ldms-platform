package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.Product;

import java.util.Locale;

public interface ProductServiceAuditable {
    Product create(Product product, Locale locale, String username);
    Product update(Product product, Locale locale, String username);
    Product delete(Product product, Locale locale);
}
