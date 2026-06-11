package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateProductRequest;
import projectlx.inventory.management.utils.requests.EditProductRequest;
import projectlx.inventory.management.utils.requests.ProductMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface ProductServiceValidator {
    ValidatorDto isCreateProductRequestValid(CreateProductRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditProductRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveProductByMultipleFilters(ProductMultipleFiltersRequest request, Locale locale);
    ValidatorDto isStringValid(String value, Locale locale);
}
