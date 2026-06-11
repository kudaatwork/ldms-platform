package projectlx.inventory.management.business.validator.api;

import projectlx.inventory.management.utils.requests.CreateProductSubCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductSubCategoryRequest;
import projectlx.inventory.management.utils.requests.ProductSubCategoryMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

public interface ProductSubCategoryServiceValidator {
    ValidatorDto isCreateProductSubCategoryRequestValid(CreateProductSubCategoryRequest request, Locale locale);
    ValidatorDto isIdValid(Long id, Locale locale);
    ValidatorDto isRequestValidForEditing(EditProductSubCategoryRequest request, Locale locale);

    // Additional
    ValidatorDto isRequestValidToRetrieveProductSubCategoryByMultipleFilters(ProductSubCategoryMultipleFiltersRequest request, Locale locale);
    ValidatorDto isStringValid(String value, Locale locale);
}
