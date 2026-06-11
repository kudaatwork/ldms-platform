package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateProductDocumentRequest;
import projectlx.inventory.management.utils.requests.EditProductDocumentRequest;
import projectlx.inventory.management.utils.requests.ProductDocumentMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface ProductDocumentServiceValidator {
    ValidatorDto isCreateProductDocumentRequestValid(CreateProductDocumentRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditProductDocumentRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveProductDocumentByMultipleFilters(ProductDocumentMultipleFiltersRequest request, Locale locale);
    ValidatorDto isStringValid(String value, Locale locale);
}
