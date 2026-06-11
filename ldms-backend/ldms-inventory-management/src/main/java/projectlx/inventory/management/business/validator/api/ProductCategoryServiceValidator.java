package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateProductCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductCategoryRequest;
import projectlx.inventory.management.utils.requests.ProductCategoryMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface ProductCategoryServiceValidator {
    ValidatorDto isCreateProductCategoryRequestValid(CreateProductCategoryRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditProductCategoryRequest request, Locale locale);
    ValidatorDto isRequestValidToRetrieveProductCategoryByMultipleFilters(ProductCategoryMultipleFiltersRequest request, Locale locale);
    ValidatorDto isStringValid(String value, Locale locale);
}
