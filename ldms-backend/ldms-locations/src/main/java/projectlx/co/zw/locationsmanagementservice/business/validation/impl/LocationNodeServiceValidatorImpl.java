package projectlx.co.zw.locationsmanagementservice.business.validation.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.LocationNodeServiceValidator;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.LocationNodeMultipleFiltersRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class LocationNodeServiceValidatorImpl implements LocationNodeServiceValidator {
    private static final Logger logger = LoggerFactory.getLogger(LocationNodeServiceValidatorImpl.class);

    @Override
    public ValidatorDto isCreateValid(CreateLocationNodeRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Create location request is null");
            return new ValidatorDto(false, null, errors);
        }
        if (request.getName() == null || request.getName().isBlank()) {
            errors.add("Location name is required");
        }
        if (request.getLocationType() == null) {
            errors.add("Location type is required");
        }
        if (request.getLocationType() == LocationType.CITY && request.getParentId() != null) {
            errors.add("CITY location nodes cannot have a parent");
        }
        if (!errors.isEmpty()) {
            logger.info("Location node create validation failed: {}", errors);
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto isEditValid(EditLocationNodeRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Edit location request is null");
            return new ValidatorDto(false, null, errors);
        }
        if (request.getId() == null || request.getId() < 1L) {
            errors.add("Valid id is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            errors.add("Location name is required");
        }
        if (request.getLocationType() == null) {
            errors.add("Location type is required");
        }
        if (request.getLocationType() == LocationType.CITY && request.getParentId() != null) {
            errors.add("CITY location nodes cannot have a parent");
        }
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }

    @Override
    public ValidatorDto isIdValid(Long id, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (id == null || id < 1L) {
            errors.add("Supplied id is invalid");
            return new ValidatorDto(false, null, errors);
        }
        return new ValidatorDto(true, null, new ArrayList<>());
    }

    @Override
    public ValidatorDto isFilterValid(LocationNodeMultipleFiltersRequest request, Locale locale) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("Filter request is null");
            return new ValidatorDto(false, null, errors);
        }
        if (request.getPage() < 0 || request.getSize() < 1) {
            errors.add("Invalid pagination values");
        }
        return errors.isEmpty() ? new ValidatorDto(true, null, new ArrayList<>()) : new ValidatorDto(false, null, errors);
    }
}
