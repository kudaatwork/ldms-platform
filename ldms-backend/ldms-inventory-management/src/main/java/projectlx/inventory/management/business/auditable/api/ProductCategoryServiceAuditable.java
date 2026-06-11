package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.ProductCategory;

import java.util.Locale;

public interface ProductCategoryServiceAuditable {
    ProductCategory create(ProductCategory productCategory, Locale locale, String username);
    ProductCategory update(ProductCategory productCategory, Locale locale, String username);
    ProductCategory delete(ProductCategory productCategory, Locale locale);
}
