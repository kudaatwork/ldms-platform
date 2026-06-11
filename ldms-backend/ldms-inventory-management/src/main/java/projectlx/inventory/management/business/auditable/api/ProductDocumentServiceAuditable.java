package projectlx.inventory.management.business.auditable.api;

import projectlx.inventory.management.model.ProductDocument;

import java.util.Locale;

public interface ProductDocumentServiceAuditable {
    ProductDocument create(ProductDocument productDocument, Locale locale, String username);
    ProductDocument update(ProductDocument productDocument, Locale locale, String username);
    ProductDocument delete(ProductDocument productDocument, Locale locale);
}
