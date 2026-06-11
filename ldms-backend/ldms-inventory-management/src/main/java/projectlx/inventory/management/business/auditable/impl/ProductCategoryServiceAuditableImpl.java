package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.ProductCategoryServiceAuditable;
import projectlx.inventory.management.model.ProductCategory;
import projectlx.inventory.management.repository.ProductCategoryRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceAuditableImpl implements ProductCategoryServiceAuditable {

    private final ProductCategoryRepository productCategoryRepository;

    @Override
    public ProductCategory create(ProductCategory productCategory, Locale locale, String username) {
        return productCategoryRepository.save(productCategory);
    }

    @Override
    public ProductCategory update(ProductCategory productCategory, Locale locale, String username) {
        return productCategoryRepository.save(productCategory);
    }

    @Override
    public ProductCategory delete(ProductCategory productCategory, Locale locale) {
        return productCategoryRepository.save(productCategory);
    }
}
