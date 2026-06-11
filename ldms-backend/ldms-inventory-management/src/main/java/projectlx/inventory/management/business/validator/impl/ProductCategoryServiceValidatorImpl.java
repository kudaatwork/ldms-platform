package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.business.validator.api.ProductCategoryServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateProductCategoryRequest;
import projectlx.inventory.management.utils.requests.EditProductCategoryRequest;
import projectlx.inventory.management.utils.requests.ProductCategoryMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrEmpty;

@RequiredArgsConstructor
public class ProductCategoryServiceValidatorImpl implements ProductCategoryServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(ProductCategoryServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateProductCategoryRequestValid(CreateProductCategoryRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateProductCategoryRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_CATEGORY_REQUEST_IS_NULL.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrEmpty(request.getName())) {
            logger.info("Validation failed: Product category name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_CATEGORY_NAME_MISSING.getCode(),
                    new String[]{}, locale));
        }

        if (isNullOrEmpty(request.getDescription())) {
            logger.info("Validation failed: Product category description is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_CATEGORY_DESCRIPTION_MISSING.getCode(),
                    new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
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
    public ValidatorDto isRequestValidForEditing(EditProductCategoryRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditProductCategoryRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_CATEGORY_REQUEST_IS_NULL.getCode(),
                    new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getProductCategoryId() == null || request.getProductCategoryId() <= 0L) {
            logger.info("Validation failed: Product category ID is invalid");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_CATEGORY_ID_INVALID.getCode(),
                    new String[]{}, locale));
        }

        if (isNullOrEmpty(request.getName())) {
            logger.info("Validation failed: Product category name is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_CATEGORY_NAME_MISSING.getCode(),
                    new String[]{}, locale));
        }

        if (isNullOrEmpty(request.getDescription())) {
            logger.info("Validation failed: Product category description is missing for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_CATEGORY_DESCRIPTION_MISSING.getCode(),
                    new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveProductCategoryByMultipleFilters(ProductCategoryMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: ProductCategoryMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is negative");
            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_PAGE_NEGATIVE.getCode(), new String[]{}, locale));
        }

        if (request.getSize() <= 0 || request.getSize() > InventoryExportSupport.MAX_FILTER_PAGE_SIZE) {
            logger.info("Validation failed: Page size is invalid (must be between 1 and {})",
                    InventoryExportSupport.MAX_FILTER_PAGE_SIZE);
            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_CATEGORY_SIZE_INVALID.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
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
