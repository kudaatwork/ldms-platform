package projectlx.co.zw.locationsmanagementservice.business.logic.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.LocationNodeServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.LocationNodeServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.LocationNode;
import projectlx.co.zw.locationsmanagementservice.repository.DistrictRepository;
import projectlx.co.zw.locationsmanagementservice.repository.LocationNodeRepository;
import projectlx.co.zw.locationsmanagementservice.repository.SuburbRepository;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateLocationNodeRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.LocationNodeResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationNodeServiceHierarchyTest {

    private LocationNodeServiceImpl service;
    private DistrictRepository districtRepository;

    @BeforeEach
    void setUp() {
        LocationNodeServiceValidator validator = mock(LocationNodeServiceValidator.class);
        LocationNodeRepository locationNodeRepository = mock(LocationNodeRepository.class);
        districtRepository = mock(DistrictRepository.class);
        SuburbRepository suburbRepository = mock(SuburbRepository.class);
        LocationNodeServiceAuditable auditable = mock(LocationNodeServiceAuditable.class);
        LocationHierarchyCascadeSoftDeleteService cascadeSoftDelete = mock(LocationHierarchyCascadeSoftDeleteService.class);
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);

        when(validator.isCreateValid(any(), any())).thenReturn(new ValidatorDto(true, null, null));
        when(auditable.create(any(), any(), any())).thenAnswer(invocation -> {
            LocationNode node = invocation.getArgument(0);
            node.setId(1L);
            return node;
        });

        service = new LocationNodeServiceImpl(
                validator,
                locationNodeRepository,
                districtRepository,
                suburbRepository,
                auditable,
                cascadeSoftDelete,
                rabbitTemplate
        );
    }

    @Test
    void cityWithoutDistrictFailsValidation() {
        CreateLocationNodeRequest request = new CreateLocationNodeRequest();
        request.setName("No District City");
        request.setLocationType(LocationType.CITY);

        LocationNodeResponse response = service.create(request, Locale.ENGLISH, "tester");
        assertFalse(response.isSuccess());
    }

    @Test
    void cityWithDistrictSucceeds() {
        District district = new District();
        district.setId(10L);
        district.setEntityStatus(EntityStatus.ACTIVE);
        district.setName("District A");
        when(districtRepository.findByIdAndEntityStatusNot(10L, EntityStatus.DELETED)).thenReturn(Optional.of(district));

        CreateLocationNodeRequest request = new CreateLocationNodeRequest();
        request.setName("District City");
        request.setLocationType(LocationType.CITY);
        request.setDistrictId(10L);

        LocationNodeResponse response = service.create(request, Locale.ENGLISH, "tester");
        assertTrue(response.isSuccess());
    }
}
