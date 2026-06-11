package projectlx.inventory.management.business.validator.impl;

import lombok.RequiredArgsConstructor;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatch;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import projectlx.inventory.management.business.logic.support.InventoryExportSupport;
import projectlx.inventory.management.business.validator.api.ProductServiceValidator;
import projectlx.inventory.management.utils.enums.I18Code;
import projectlx.inventory.management.utils.requests.CreateProductRequest;
import projectlx.inventory.management.utils.requests.EditProductRequest;
import projectlx.inventory.management.utils.requests.ProductMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrEmpty;

@RequiredArgsConstructor
public class ProductServiceValidatorImpl implements ProductServiceValidator {

    @Value("${constants.max-image-size:15MB}")
    private String maxImageSize;
    private static Logger logger = LoggerFactory.getLogger(ProductServiceValidatorImpl.class);
    private final List<String> executableExtensions =
            Arrays.asList("application/x-executable", "application/x-msdos-program");

    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateProductRequestValid(CreateProductRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {

            logger.info("Validation failed: CreateProductRequest is null");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_REQUEST_IS_NULL.getCode(),
                    new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrEmpty(request.getName())) {

            logger.info("Validation failed: Product name is missing");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_NAME_MISSING.getCode(), new String[]{},
                    locale));
        }

        if (isNullOrEmpty(request.getProductCode())) {

            logger.info("Validation failed: Product code is missing");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_CODE_MISSING.getCode(), new String[]{},
                    locale));
        }

        BigDecimal price = request.getPrice();

        if (price == null || price.signum() < 0) {

            logger.info("Validation failed: Product price is invalid");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_PRICE_INVALID.getCode(), new String[]{},
                    locale));
        }

        if (request.getUnitOfMeasure() == null) {

            logger.info("Validation failed: Unit of measure is missing");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_UNIT_OF_MEASURE_MISSING.getCode(),
                    new String[]{}, locale));
        }

        if (request.getProductCategoryId() == null || request.getProductCategoryId() <= 0L) {

            logger.info("Validation failed: Category ID is invalid");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_CATEGORY_ID_INVALID.getCode(),
                    new String[]{}, locale));
        }

        if (request.getSupplierId() == null || request.getSupplierId() <= 0L) {

            logger.info("Validation failed: Supplier ID is invalid");

            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_SUPPLIER_ID_INVALID.getCode(),
                    new String[]{}, locale));
        }

        if (request.getImageUpload() != null && !request.getImageUpload().isEmpty()) {
            try {
                if (!isImageValid(request.getImageUpload())) {

                    logger.info("Validation failed: Create product image upload is invalid");

                    errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_PRODUCT_INVALID_IMAGE_UPLOAD.getCode(),
                            new String[]{}, locale));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
    public ValidatorDto isRequestValidForEditing(EditProductRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {

            logger.info("Validation failed: EditProductRequest is null");

            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_REQUEST_IS_NULL.getCode(),
                    new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        if (request.getProductId() == null || request.getProductId() <= 0L) {

            logger.info("Validation failed: Product ID is invalid");

            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_ID_INVALID.getCode(),
                    new String[]{}, locale));
        }

        if (request.getCategoryId() != null && request.getCategoryId() <= 0L) {

            logger.info("Validation failed: Category ID is invalid for editing");

            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_CATEGORY_ID_INVALID.getCode(),
                    new String[]{}, locale));
        }

        if (request.getSupplierId() != null && request.getSupplierId() <= 0L) {

            logger.info("Validation failed: Supplier ID is invalid for editing");

            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_SUPPLIER_ID_INVALID.getCode(),
                    new String[]{}, locale));
        }

        if (request.getPrice() != null && request.getPrice().signum() < 0) {

            logger.info("Validation failed: Product price is invalid for editing");

            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_PRICE_INVALID.getCode(), new String[]{},
                    locale));
        }

        if (request.getImageUpload() != null && !request.getImageUpload().isEmpty()) {
            try {
                if (!isImageValid(request.getImageUpload())) {

                    logger.info("Validation failed: Update product image upload is invalid");

                    errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_PRODUCT_INVALID_IMAGE_UPLOAD.getCode(),
                            new String[]{}, locale));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveProductByMultipleFilters(ProductMultipleFiltersRequest request, Locale locale) {

        List<String> errors = new ArrayList<>();

        if (request == null) {

            logger.info("Validation failed: ProductMultipleFiltersRequest is null");

            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_REQUEST_IS_NULL.getCode(), new String[]{}, locale));

            return new ValidatorDto(false, null, errors);
        }

        if (request.getPage() < 0) {

            logger.info("Validation failed: Page number is negative");

            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_PAGE_NEGATIVE.getCode(), new String[]{}, locale));
        }

        if (request.getSize() <= 0 || request.getSize() > InventoryExportSupport.MAX_FILTER_PAGE_SIZE) {

            logger.info("Validation failed: Page size is invalid (must be between 1 and {})",
                    InventoryExportSupport.MAX_FILTER_PAGE_SIZE);

            errors.add(messageService.getMessage(I18Code.MESSAGE_PRODUCT_SIZE_INVALID.getCode(), new String[]{}, locale));
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

            errors.add(messageService.getMessage(I18Code.MESSAGE_STRING_SUPPLIED_IS_NULL.getCode(), new String[]{},
                    locale));

            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    private boolean isImageValid(MultipartFile multipartFile) throws IOException {

        if (multipartFile == null) {
            return false;
        }

        byte[] file = multipartFile.getBytes();

        if (checkImageSizeLimit(file) || checkIfFileTypeAndExtensionAreValid(file)) {
            return false;
        }

        return true;
    }

    private Boolean checkImageSizeLimit(byte[] fileData) {

        return convertKbToBytes(maxImageSize) <= fileData.length;
    }

    private Boolean checkIfFileTypeAndExtensionAreValid(byte[] fileData) {

        try {
            MagicMatch match = Magic.getMagicMatch(fileData, false);

            if (executableExtensions.contains(match.getExtension().toUpperCase())) {
                return true;
            }

        } catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
            return false;
        }
        return false;
    }

    private long convertKbToBytes(String maxFileSize) {

        String size = maxFileSize.substring(0, maxFileSize.length() - 2);

        return Long.valueOf(size) * 1000;
    }
}
