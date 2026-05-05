package projectlx.co.zw.locationsmanagementservice.business.validation.impl;

import org.junit.jupiter.api.Test;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.EditLocationNodeRequest;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationNodeServiceValidatorImplTest {

    private final LocationNodeServiceValidatorImpl validator = new LocationNodeServiceValidatorImpl();

    @Test
    void createCityWithParentIdIsInvalid() {
        CreateLocationNodeRequest request = new CreateLocationNodeRequest();
        request.setName("X");
        request.setLocationType(LocationType.CITY);
        request.setParentId(99L);
        ValidatorDto dto = validator.isCreateValid(request, Locale.ENGLISH);
        assertFalse(dto.getSuccess());
    }

    @Test
    void editCityWithParentIdIsInvalid() {
        EditLocationNodeRequest request = new EditLocationNodeRequest();
        request.setId(1L);
        request.setName("X");
        request.setLocationType(LocationType.CITY);
        request.setParentId(99L);
        ValidatorDto dto = validator.isEditValid(request, Locale.ENGLISH);
        assertFalse(dto.getSuccess());
    }

    @Test
    void createCityWithoutParentIsValid() {
        CreateLocationNodeRequest request = new CreateLocationNodeRequest();
        request.setName("X");
        request.setLocationType(LocationType.CITY);
        ValidatorDto dto = validator.isCreateValid(request, Locale.ENGLISH);
        assertTrue(dto.getSuccess());
    }
}
