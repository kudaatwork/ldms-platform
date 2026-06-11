package projectlx.inventory.management.business.auditable.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.auditable.api.ProductDocumentServiceAuditable;
import projectlx.inventory.management.model.ProductDocument;
import projectlx.inventory.management.repository.ProductDocumentRepository;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductDocumentServiceAuditableImpl implements ProductDocumentServiceAuditable {

    private final ProductDocumentRepository productDocumentRepository;

    @Override
    public ProductDocument create(ProductDocument productDocument, Locale locale, String username) {
        return productDocumentRepository.save(productDocument);
    }

    @Override
    public ProductDocument update(ProductDocument productDocument, Locale locale, String username) {
        return productDocumentRepository.save(productDocument);
    }

    @Override
    public ProductDocument delete(ProductDocument productDocument, Locale locale) {
        return productDocumentRepository.save(productDocument);
    }
}
