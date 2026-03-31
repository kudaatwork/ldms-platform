package projectlx.co.zw.locationsmanagementservice.business.logic.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Service;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.AddressService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.CountryService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.DistrictService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.GeoCoordinatesService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocationIngestionService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.ProvinceService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.SuburbService;
import projectlx.co.zw.locationsmanagementservice.model.Address;
import projectlx.co.zw.locationsmanagementservice.model.AdministrativeLevel;
import projectlx.co.zw.locationsmanagementservice.model.Country;
import projectlx.co.zw.locationsmanagementservice.model.District;
import projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates;
import projectlx.co.zw.locationsmanagementservice.model.Province;
import projectlx.co.zw.locationsmanagementservice.model.Suburb;
import projectlx.co.zw.locationsmanagementservice.repository.AddressRepository;
import projectlx.co.zw.locationsmanagementservice.repository.AdministrativeLevelRepository;
import projectlx.co.zw.locationsmanagementservice.repository.CountryRepository;
import projectlx.co.zw.locationsmanagementservice.repository.DistrictRepository;
import projectlx.co.zw.locationsmanagementservice.repository.ProvinceRepository;
import projectlx.co.zw.locationsmanagementservice.repository.SuburbRepository;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateAddressRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateCountryRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateDistrictRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateGeoCoordinatesRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateProvinceRequest;
import projectlx.co.zw.locationsmanagementservice.utils.requests.CreateSuburbRequest;
import projectlx.co.zw.locationsmanagementservice.utils.responses.AddressResponse;
import projectlx.co.zw.locationsmanagementservice.utils.responses.CountryResponse;
import projectlx.co.zw.locationsmanagementservice.utils.responses.DistrictResponse;
import projectlx.co.zw.locationsmanagementservice.utils.responses.GeoCoordinatesResponse;
import projectlx.co.zw.locationsmanagementservice.utils.responses.ProvinceResponse;
import projectlx.co.zw.locationsmanagementservice.utils.responses.SuburbResponse;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Injects all final fields via constructor
public class LocationIngestionServiceImpl implements LocationIngestionService {

    // --- Repositories for FIND operations ---
    private final AddressRepository addressRepository;
    private final SuburbRepository suburbRepository;
    private final DistrictRepository districtRepository;
    private final ProvinceRepository provinceRepository;
    private final CountryRepository countryRepository;
    private final AdministrativeLevelRepository administrativeLevelRepository;
    private final ModelMapper modelMapper;

    // --- Injected Services for CREATE operations ---
    private final CountryService countryService;
    private final ProvinceService provinceService;
    private final DistrictService districtService;
    private final SuburbService suburbService;
    private final GeoCoordinatesService geoCoordinatesService; // Added for consistency
    private final AddressService addressService;


    @Override
    @Transactional
    public Address ingestGooglePlace(GooglePlaceDetails details, Locale locale, String username) {

        Map<String, AddressComponent> componentMap = details.getAddressComponents().stream()
                .filter(c -> c.getTypes() != null && !c.getTypes().isEmpty())
                .collect(Collectors.toMap(c -> c.getTypes().get(0), Function.identity(), (c1, c2) -> c1));

        Country country = findOrCreateCountry(componentMap.get("country"), locale, username);
        Province province = findOrCreateProvince(componentMap.get("administrative_area_level_1"), country, locale, username);
        District district = findOrCreateDistrict(componentMap.get("locality"), province, locale, username);
        Suburb suburb = findOrCreateSuburb(componentMap.get("sublocality_level_1"), district, locale, username);

        GeoCoordinates coordinates = createGeoCoordinates(details.getGeometry().getLocation(), locale, username);

        CreateAddressRequest addressRequest = new CreateAddressRequest();
        String streetNumber = Optional.ofNullable(componentMap.get("street_number")).map(AddressComponent::getLongName).orElse("");
        String route = Optional.ofNullable(componentMap.get("route")).map(AddressComponent::getLongName).orElse("");

        addressRequest.setLine1(String.join(" ", streetNumber, route).trim());
        addressRequest.setPostalCode(Optional.ofNullable(componentMap.get("postal_code")).map(AddressComponent::getLongName).orElse(null));

        if (suburb != null) {
            addressRequest.setSuburbId(suburb.getId());
        }
        if (coordinates != null) {
            addressRequest.setGeoCoordinatesId(coordinates.getId());
        }

        AddressResponse response = addressService.create(addressRequest, locale, username);
        if (response == null || response.getAddressDto() == null) {
            throw new IllegalStateException("Failed to create final address record.");
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        Address address = modelMapper.map(response.getAddressDto(), Address.class);
        return address;
    }

    private Country findOrCreateCountry(AddressComponent component, Locale locale, String username) {

        if (component == null) {
            throw new IllegalArgumentException("Country information is missing from Google Places data.");
        }
        return countryRepository.findByIsoAlpha2CodeAndEntityStatusNot(component.getShortName(), EntityStatus.DELETED)
                .orElseGet(() -> {
                    CreateCountryRequest createRequest = new CreateCountryRequest();
                    createRequest.setName(component.getLongName());
                    createRequest.setIsoAlpha2Code(component.getShortName());

                    CountryResponse response = countryService.create(createRequest, locale, username);
                    if (response == null || response.getCountryDto() == null) {
                        throw new IllegalStateException("Failed to create country: " + component.getLongName());
                    }

                    modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
                    Country country = modelMapper.map(response.getCountryDto(), Country.class);
                    return country;
                });
    }

    private Province findOrCreateProvince(AddressComponent component, Country country, Locale locale, String username) {

        if (component == null || country == null) return null;

        return provinceRepository.findByNameAndCountryAndEntityStatusNot(component.getLongName(), country, EntityStatus.DELETED)
                .orElseGet(() -> {
                    AdministrativeLevel adminLevel = administrativeLevelRepository.findByNameAndEntityStatusNot("Province", EntityStatus.DELETED)
                            .orElseThrow(() -> new IllegalStateException("'Province' AdministrativeLevel not found."));

                    CreateProvinceRequest createRequest = new CreateProvinceRequest();
                    createRequest.setName(component.getLongName());
                    createRequest.setCountryId(country.getId());
                    createRequest.setAdministrativeLevelId(adminLevel.getId());

                    ProvinceResponse response = provinceService.create(createRequest, locale, username);
                    if (response == null || response.getProvinceDto() == null) {
                        throw new IllegalStateException("Failed to create province: " + component.getLongName());
                    }

                    modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
                    Province province = modelMapper.map(response.getProvinceDto(), Province.class);
                    return province;
                });
    }

    private District findOrCreateDistrict(AddressComponent component, Province province, Locale locale, String username) {

        if (component == null || province == null) return null;

        return districtRepository.findByNameAndProvinceAndEntityStatusNot(component.getLongName(), province, EntityStatus.DELETED)
                .orElseGet(() -> {
                    AdministrativeLevel adminLevel = administrativeLevelRepository.findByNameAndEntityStatusNot("District", EntityStatus.DELETED)
                            .orElseThrow(() -> new IllegalStateException("'District' AdministrativeLevel not found."));

                    CreateDistrictRequest createRequest = new CreateDistrictRequest();
                    createRequest.setName(component.getLongName());
                    createRequest.setProvinceId(province.getId());
                    createRequest.setAdministrativeLevelId(adminLevel.getId());

                    DistrictResponse response = districtService.create(createRequest, locale, username);
                    if (response == null || response.getDistrictDto() == null) {
                        throw new IllegalStateException("Failed to create district: " + component.getLongName());
                    }

                    modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
                    District district = modelMapper.map(response.getDistrictDto(), District.class);
                    return district;
                });
    }

    private Suburb findOrCreateSuburb(AddressComponent component, District district, Locale locale, String username) {

        if (component == null || district == null) return null;

        return (Suburb) suburbRepository.findByNameAndDistrictAndEntityStatusNot(component.getLongName(), district, EntityStatus.DELETED)
                .orElseGet(() -> {
                    AdministrativeLevel adminLevel = administrativeLevelRepository.findByNameAndEntityStatusNot("Suburb / Ward", EntityStatus.DELETED)
                            .orElseThrow(() -> new IllegalStateException("'Suburb / Ward' AdministrativeLevel not found."));

                    CreateSuburbRequest createRequest = new CreateSuburbRequest();
                    createRequest.setName(component.getLongName());
                    createRequest.setDistrictId(district.getId());
                    createRequest.setAdministrativeLevelId(adminLevel.getId());

                    SuburbResponse response = suburbService.create(createRequest, locale, username);

                    if (response == null || response.getSuburbDto() == null) {
                        throw new IllegalStateException("Failed to create suburb: " + component.getLongName());
                    }

                    modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
                    Suburb suburb = modelMapper.map(response.getSuburbDto(), Suburb.class);
                    return suburb;
                });
    }

    private GeoCoordinates createGeoCoordinates(Location location, Locale locale, String username) {

        if (location == null) return null;

        CreateGeoCoordinatesRequest createRequest = new CreateGeoCoordinatesRequest();
        createRequest.setLatitude(location.getLat());
        createRequest.setLongitude(location.getLng());

        GeoCoordinatesResponse response = geoCoordinatesService.create(createRequest, locale, username);

        if (response == null || response.getGeoCoordinatesDto() == null) {
            throw new IllegalStateException("Failed to create geo-coordinates for the address.");
        }

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        GeoCoordinates geoCoordinates = modelMapper.map(response.getGeoCoordinatesDto(), GeoCoordinates.class);
        return geoCoordinates;
    }
}
