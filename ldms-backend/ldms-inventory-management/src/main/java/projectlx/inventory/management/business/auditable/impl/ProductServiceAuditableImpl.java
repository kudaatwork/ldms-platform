package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.ProductServiceAuditable;
import projectlx.inventory.management.model.Product;
import projectlx.inventory.management.repository.ProductRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductServiceAuditableImpl implements ProductServiceAuditable {

    private final ProductRepository productRepository;

    @Override
    public Product create(Product product, Locale locale, String username) {
        return productRepository.save(product);
    }

    @Override
    public Product update(Product product, Locale locale, String username) {
        return productRepository.save(product);
    }

    @Override
    public Product delete(Product product, Locale locale) {
        return productRepository.save(product);
    }
}
