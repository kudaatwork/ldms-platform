package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.inventory.management.business.validator.api.ProductDocumentServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateProductDocumentRequest;
import projectlx.inventory.management.utils.requests.EditProductDocumentRequest;
import projectlx.inventory.management.utils.requests.ProductDocumentMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrEmpty;

@RequiredArgsConstructor
public class ProductDocumentServiceValidatorImpl implements ProductDocumentServiceValidator {

    private static final Logger logger = LoggerFactory.getLogger(ProductDocumentServiceValidatorImpl.class);

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateProductDocumentRequestValid(CreateProductDocumentRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateProductDocumentRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_DOCUMENT_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getProductId() == null || request.getProductId() <= 0L) {
            logger.info("Validation failed: Product ID is invalid for ProductDocument");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_DOCUMENT_PRODUCT_ID_INVALID.getCode(), new String[]{}, locale));
        }

        if (isNullOrEmpty(request.getName())) {
            logger.info("Validation failed: Product document name is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_DOCUMENT_NAME_MISSING.getCode(), new String[]{}, locale));
        }

        // Upload is optional to allow creation via CSV import without a file

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
    public ValidatorDto isRequestValidForEditing(EditProductDocumentRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditProductDocumentRequest is null");
            // Reusing invalid request message for updates would require dedicated codes; omitted per scope
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_DOCUMENT_INVALID_REQUEST.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getProductDocumentId() == null || request.getProductDocumentId() <= 0L) {
            logger.info("Validation failed: Product document ID is invalid for editing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveProductDocumentByMultipleFilters(ProductDocumentMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: ProductDocumentMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is negative");
            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_PAGE_NEGATIVE.getCode(), new String[]{}, locale));
        }

        if (request.getSize() <= 0 || request.getSize() > 100) {
            logger.info("Validation failed: Page size is invalid (must be between 1 and 100)");
            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_DOCUMENT_SIZE_INVALID.getCode(), new String[]{}, locale));
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
