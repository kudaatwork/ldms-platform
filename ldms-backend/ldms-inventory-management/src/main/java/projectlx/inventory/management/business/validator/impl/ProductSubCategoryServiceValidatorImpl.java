package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.business.validator.api.ProductSubCategoryServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateProductSubCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductSubCategoryRequest;
import projectlx.inventory.management.utils.requests.ProductSubCategoryMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrEmpty;

@RequiredArgsConstructor
public class ProductSubCategoryServiceValidatorImpl implements ProductSubCategoryServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(ProductSubCategoryServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateProductSubCategoryRequestValid(CreateProductSubCategoryRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateProductSubCategoryRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_SUB_CATEGORY_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getCategoryId() == null || request.getCategoryId() <= 0L) {
            logger.info("Validation failed: Category ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_SUB_CATEGORY_CATEGORY_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (isNullOrEmpty(request.getName())) {
            logger.info("Validation failed: Product sub-category name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_SUB_CATEGORY_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (isNullOrEmpty(request.getDescription())) {
            logger.info("Validation failed: Product sub-category description is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_SUB_CATEGORY_DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        return errors.isEmpty() ? new ValidatorDto(true, null, null) : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (id == null || id <= 0L) {
            logger.info("Validation failed: ID is null or less than or equal to 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditProductSubCategoryRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditProductSubCategoryRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_SUB_CATEGORY_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getProductSubCategoryId() == null || request.getProductSubCategoryId() <= 0L) {
            logger.info("Validation failed: Product sub-category ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_SUB_CATEGORY_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getCategoryId() == null || request.getCategoryId() <= 0L) {
            logger.info("Validation failed: Category ID is invalid for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_SUB_CATEGORY_CATEGORY_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (isNullOrEmpty(request.getName())) {
            logger.info("Validation failed: Product sub-category name is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_SUB_CATEGORY_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        if (isNullOrEmpty(request.getDescription())) {
            logger.info("Validation failed: Product sub-category description is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_SUB_CATEGORY_DESCRIPTION_MISSING.getCode(), new String[]{}, locale));
        }

        return errors.isEmpty() ? new ValidatorDto(true, null, null) : new ValidatorDto(false, null, errors);
    }

    // Additional validators following UserGroupServiceImpl/ProductCategoryServiceImpl
    public ValidatorDto isRequestValidToRetrieveProductSubCategoryByMultipleFilters(ProductSubCategoryMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: ProductSubCategoryMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is less than minimum");
            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_PAGE_OUT_OF_BOUNDS.getCode(), new String[]{}, locale));
        }

        if (request.getSize() <= 0 || request.getSize() > InventoryExportSupport.MAX_FILTER_PAGE_SIZE) {
            logger.info("Validation failed: Page size is invalid (must be between 1 and {})",
                    InventoryExportSupport.MAX_FILTER_PAGE_SIZE);
            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_SUB_CATEGORY_INVALID_MULTIPLE_FILTERS_REQUEST.getCode(), new String[]{}, locale));
        }

        return errors.isEmpty() ? new ValidatorDto(true, null, null) : new ValidatorDto(false, null, errors);
    }

    public ValidatorDto isStringValid(String value, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (isNullOrEmpty(value)) {
            logger.info("Validation failed: String is null or empty");
            errors.add(messageService.getMessage(I18Code.MESSAGE_STRING_SUPPLIED_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, null);
    }
}
