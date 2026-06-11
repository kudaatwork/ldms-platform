package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.ProductSubCategory;

import java.util.Locale;

public interface ProductSubCategoryServiceAuditable {
    ProductSubCategory create(ProductSubCategory productSubCategory, Locale locale, String username);
    ProductSubCategory update(ProductSubCategory productSubCategory, Locale locale, String username);
    ProductSubCategory delete(ProductSubCategory productSubCategory, Locale locale);
}
