package projectlx.co.zw.locationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.GeoCoordinatesServiceValidator;
import projectlx.co.zw.locationsmanagementservice.utils.enums.I18Code;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.GeoCoordinatesMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static projectlx.co.zw.shared_library.utils.globalvalidators.Validators.isNullOrLessThanOne;

@Service
@RequiredArgsConstructor
public class GeoCoordinatesServiceValidatorImpl implements GeoCoordinatesServiceValidator {

    private final Logger logger = LoggerFactory.getLogger(GeoCoordinatesServiceValidatorImpl.class);
    private final MessageService messageService;

    @Override
    public ValidatorDto isCreateGeoCoordinatesRequestValid(CreateGeoCoordinatesRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: CreateGeoCoordinatesRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getLatitude() == null) {
            logger.info("Validation failed: Geo coordinates latitude is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_LATITUDE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getLongitude() == null) {
            logger.info("Validation failed: Geo coordinates longitude is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_CREATE_GEO_COORDINATES_LONGITUDE_MISSING.getCode(), new String[]{}, locale));
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

        if (isNullOrLessThanOne(id)) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        return new ValidatorDto(true, null, null);
    }

    @Override
    public ValidatorDto isRequestValidForEditing(EditGeoCoordinatesRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: EditGeoCoordinatesRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_GEO_COORDINATES_REQUEST_IS_NULL.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (isNullOrLessThanOne(request.getId())) {
            logger.info("Validation failed: ID is null or less than one");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
        }

        if (request.getLatitude() == null) {
            logger.info("Validation failed: Geo coordinates latitude is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_GEO_COORDINATES_LATITUDE_MISSING.getCode(), new String[]{}, locale));
        }

        if (request.getLongitude() == null) {
            logger.info("Validation failed: Geo coordinates longitude is missing");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_GEO_COORDINATES_LONGITUDE_MISSING.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }

    @Override
    public ValidatorDto isRequestValidToRetrieveGeoCoordinatesByMultipleFilters(GeoCoordinatesMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            logger.info("Validation failed: GeoCoordinatesMultipleFiltersRequest is null");
            errors.add(messageService.getMessage(I18Code.MESSAGE_ID_SUPPLIED_INVALID.getCode(), new String[]{}, locale));
            return new ValidatorDto(false, null, errors);
        }

        if (request.getPage() < 0) {
            logger.info("Validation failed: Page number is less than 0");
            errors.add(messageService.getMessage(I18Code.MESSAGE_UPDATE_GEO_COORDINATES_INVALID_REQUEST.getCode(), new String[]{}, locale));
        }

        if (errors.isEmpty()) {
            return new ValidatorDto(true, null, null);
        } else {
            return new ValidatorDto(false, null, errors);
        }
    }
}
