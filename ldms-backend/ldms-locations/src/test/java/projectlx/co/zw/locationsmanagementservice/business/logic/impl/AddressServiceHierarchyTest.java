package projectlx.co.zw.locationsmanagementservice.business.logic.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.AddressServiceAuditable;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.AddressServiceValidator;
import projectlx.co.zw.locationsmanagementservice.model.Address;
import projectlx.co.zw.locationsmanagementservice.model.City;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.LocationNode;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.locationsmanagementservice.repository.AddressRepository;
import projectlx.co.zw.locationsmanagementservice.repository.CityRepository;
import projectlx.co.zw.locationsmanagementservice.repository.GeoCoordinatesRepository;
import projectlx.co.zw.locationsmanagementservice.repository.LocationNodeRepository;
import projectlx.co.zw.locationsmanagementservice.repository.SuburbRepository;
import projectlx.co.zw.locationsmanagementservice.utils.enums.LocationType;
import projectlx.co.zw.locationsmanagementservice.utils.enums.SettlementType;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.AddressResponse;
import projectlx.co.zw.shared_library.utils.dtos.ValidatorDto;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddressServiceHierarchyTest {

    private AddressServiceImpl service;
    private SuburbRepository suburbRepository;
    private LocationNodeRepository locationNodeRepository;
    private CityRepository cityRepository;
    private AddressServiceAuditable addressServiceAuditable;

    @BeforeEach
    void setUp() {
        AddressServiceValidator validator = mock(AddressServiceValidator.class);
        AddressRepository addressRepository = mock(AddressRepository.class);
        cityRepository = mock(CityRepository.class);
        suburbRepository = mock(SuburbRepository.class);
        locationNodeRepository = mock(LocationNodeRepository.class);
        GeoCoordinatesRepository geoCoordinatesRepository = mock(GeoCoordinatesRepository.class);
        addressServiceAuditable = mock(AddressServiceAuditable.class);
        MessageService messageService = mock(MessageService.class);

        when(validator.isCreateAddressRequestValid(any(), any())).thenReturn(new ValidatorDto(true, null, null));
        when(addressRepository.findAll()).thenReturn(List.of());
        when(messageService.getMessage(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(addressServiceAuditable.create(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));

        service = new AddressServiceImpl(
                validator,
                addressRepository,
                cityRepository,
                suburbRepository,
                locationNodeRepository,
                geoCoordinatesRepository,
                addressServiceAuditable,
                messageService
        );
    }

    @Test
    void createWithSuburbSettlementSucceeds() {
        Suburb suburb = new Suburb();
        suburb.setId(10L);
        when(suburbRepository.findByIdFetchingCityContext(10L, EntityStatus.DELETED)).thenReturn(Optional.of(suburb));

        CreateAddressRequest request = new CreateAddressRequest();
        request.setLine1("12 Main Road");
        request.setPostalCode("0000");
        request.setSettlementType(SettlementType.SUBURB);
        request.setSettlementId(10L);

        AddressResponse response = service.create(request, Locale.ENGLISH, "tester");
        assertTrue(response.isSuccess());
        assertEquals(SettlementType.SUBURB, response.getAddressDto().getSettlementType());
        assertEquals(10L, response.getAddressDto().getSuburbId());
    }

    @Test
    void createWithSuburbSettlementSetsCityFromLegacyCityLocationNode() {
        District district = new District();
        district.setId(5L);

        LocationNode cityNode = new LocationNode();
        cityNode.setId(99L);
        cityNode.setName("Harare");

        Suburb suburb = new Suburb();
        suburb.setId(10L);
        suburb.setDistrict(district);
        suburb.setCityLocationNode(cityNode);

        City city = new City();
        city.setId(99L);
        city.setName("Harare");
        city.setDistrict(district);
        city.setEntityStatus(EntityStatus.ACTIVE);

        when(suburbRepository.findByIdFetchingCityContext(10L, EntityStatus.DELETED)).thenReturn(Optional.of(suburb));
        when(cityRepository.findByIdAndEntityStatusNot(99L, EntityStatus.DELETED)).thenReturn(Optional.of(city));
        when(addressServiceAuditable.create(any(), any(), any())).thenAnswer(inv -> {
            Address saved = inv.getArgument(0);
            assertEquals(99L, saved.getCity().getId());
            return saved;
        });

        CreateAddressRequest request = new CreateAddressRequest();
        request.setLine1("12 Main Road");
        request.setPostalCode("0000");
        request.setSettlementType(SettlementType.SUBURB);
        request.setSettlementId(10L);

        AddressResponse response = service.create(request, Locale.ENGLISH, "tester");
        assertTrue(response.isSuccess());
        assertEquals(99L, response.getAddressDto().getCityId());
        assertEquals("Harare", response.getAddressDto().getCityName());
    }

    @Test
    void createWithVillageSettlementSucceeds() {
        LocationNode village = new LocationNode();
        village.setId(20L);
        village.setName("Village A");
        village.setLocationType(LocationType.VILLAGE);
        District district = new District();
        district.setId(5L);
        village.setDistrict(district);
        when(locationNodeRepository.findByIdAndLocationTypeAndEntityStatusNot(
                eq(20L), eq(LocationType.VILLAGE), eq(EntityStatus.DELETED))
        ).thenReturn(Optional.of(village));

        CreateAddressRequest request = new CreateAddressRequest();
        request.setLine1("Plot 7");
        request.setPostalCode("0001");
        request.setSettlementType(SettlementType.VILLAGE);
        request.setSettlementId(20L);

        AddressResponse response = service.create(request, Locale.ENGLISH, "tester");
        assertTrue(response.isSuccess());
        assertEquals(SettlementType.VILLAGE, response.getAddressDto().getSettlementType());
        assertEquals(20L, response.getAddressDto().getVillageId());
    }
}
