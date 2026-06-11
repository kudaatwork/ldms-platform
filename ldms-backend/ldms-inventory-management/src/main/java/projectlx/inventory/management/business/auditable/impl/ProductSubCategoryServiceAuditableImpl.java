package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import projectlx.inventory.management.business.auditable.api.ProductSubCategoryServiceAuditable;
import projectlx.inventory.management.model.ProductSubCategory;
import projectlx.inventory.management.repository.ProductSubCategoryRepository;

import java.util.Locale;

@RequiredArgsConstructor
public class ProductSubCategoryServiceAuditableImpl implements ProductSubCategoryServiceAuditable {

    private final ProductSubCategoryRepository productSubCategoryRepository;

    @Override
    public ProductSubCategory create(ProductSubCategory productSubCategory, Locale locale, String username) {
        return productSubCategoryRepository.save(productSubCategory);
    }

    @Override
    public ProductSubCategory update(ProductSubCategory productSubCategory, Locale locale, String username) {
        return productSubCategoryRepository.save(productSubCategory);
    }

    @Override
    public ProductSubCategory delete(ProductSubCategory productSubCategory, Locale locale) {
        return productSubCategoryRepository.save(productSubCategory);
    }
}
