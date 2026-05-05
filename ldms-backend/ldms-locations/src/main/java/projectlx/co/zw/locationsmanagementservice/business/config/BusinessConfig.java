package projectlx.co.zw.locationsmanagementservice.business.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.modelmapper.ModelMapper;
import projectlx.co.zw.locationsmanagementservice.business.auditable.api.*;
import projectlx.co.zw.locationsmanagementservice.business.auditable.impl.*;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.*;
import projectlx.co.zw.locationsmanagementservice.business.logic.impl.*;
import projectlx.co.zw.locationsmanagementservice.business.validation.api.*;
import projectlx.co.zw.locationsmanagementservice.business.validation.impl.*;
import projectlx.co.zw.locationsmanagementservice.repository.*;
import projectlx.co.zw.shared_library.utils.i18.api.MessageService;

@Configuration
public class BusinessConfig {

    @Bean
    public AddressServiceAuditable addressServiceAuditable(AddressRepository addressRepository) {
        return new AddressServiceAuditableImpl(addressRepository);
    }

    @Bean
    public AdministrativeLevelServiceAuditable administrativeLevelServiceAuditable(AdministrativeLevelRepository administrativeLevelRepository) {
        return new AdministrativeLevelServiceAuditableImpl(administrativeLevelRepository);
    }

    @Bean
    public CountryServiceAuditable countryServiceAuditable(CountryRepository countryRepository) {
        return new CountryServiceAuditableImpl(countryRepository);
    }

    @Bean
    public DistrictServiceAuditable districtServiceAuditable(DistrictRepository districtRepository) {
        return new DistrictServiceAuditableImpl(districtRepository);
    }

    @Bean
    public GeoCoordinatesServiceAuditable geoCoordinatesServiceAuditable(GeoCoordinatesRepository geoCoordinatesRepository) {
        return new GeoCoordinatesServiceAuditableImpl(geoCoordinatesRepository);
    }

    @Bean
    public LanguageServiceAuditable languageServiceAuditable(LanguageRepository languageRepository) {
        return new LanguageServiceAuditableImpl(languageRepository);
    }

    @Bean
    public LocalizedNameServiceAuditable localizedNameServiceAuditable(LocalizedNameRepository localizedNameRepository) {
        return new LocalizedNameServiceAuditableImpl(localizedNameRepository);
    }

    @Bean
    public ProvinceServiceAuditable provinceServiceAuditable(ProvinceRepository provinceRepository) {
        return new ProvinceServiceAuditableImpl(provinceRepository);
    }

    @Bean
    public SuburbServiceAuditable suburbServiceAuditable(SuburbRepository suburbRepository) {
        return new SuburbServiceAuditableImpl(suburbRepository);
    }

    @Bean
    public LocationNodeServiceAuditable locationNodeServiceAuditable(LocationNodeRepository locationNodeRepository) {
        return new LocationNodeServiceAuditableImpl(locationNodeRepository);
    }

    @Bean
    public CountryServiceValidator countryServiceValidator(MessageService messageService) {
        return new CountryServiceValidatorImpl(messageService);
    }

    @Bean
    public CountryService countryService(CountryServiceValidator countryServiceValidator, 
                                         CountryRepository countryRepository,
                                         GeoCoordinatesRepository geoCoordinatesRepository,
                                         CountryServiceAuditable countryServiceAuditable,
                                         MessageService messageService,
                                         ModelMapper modelMapper,
                                         LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService) {
        return new CountryServiceImpl(countryServiceValidator,
                                     countryRepository,
                                     geoCoordinatesRepository,
                                     countryServiceAuditable,
                                     messageService,
                                     modelMapper,
                                     locationHierarchyCascadeSoftDeleteService);
    }

    @Bean
    public AddressServiceValidator addressServiceValidator(MessageService messageService) {
        return new AddressServiceValidatorImpl(messageService);
    }

    @Bean
    public AddressService addressService(AddressServiceValidator addressServiceValidator,
                                        AddressRepository addressRepository,
                                        CityRepository cityRepository,
                                        SuburbRepository suburbRepository,
                                        LocationNodeRepository locationNodeRepository,
                                        GeoCoordinatesRepository geoCoordinatesRepository,
                                        AddressServiceAuditable addressServiceAuditable,
                                        MessageService messageService) {
        return new AddressServiceImpl(addressServiceValidator,
                                     addressRepository,
                                     cityRepository,
                                     suburbRepository,
                                     locationNodeRepository,
                                     geoCoordinatesRepository,
                                     addressServiceAuditable,
                                     messageService);
    }

    @Bean
    public AdministrativeLevelServiceValidator administrativeLevelServiceValidator(MessageService messageService) {
        return new AdministrativeLevelServiceValidatorImpl(messageService);
    }

    @Bean
    public AdministrativeLevelService administrativeLevelService(AdministrativeLevelServiceValidator administrativeLevelServiceValidator,
                                                               AdministrativeLevelRepository administrativeLevelRepository,
                                                               GeoCoordinatesRepository geoCoordinatesRepository,
                                                               CountryRepository countryRepository,
                                                               AdministrativeLevelServiceAuditable administrativeLevelServiceAuditable,
                                                               MessageService messageService) {
        return new AdministrativeLevelServiceImpl(administrativeLevelServiceValidator,
                                                administrativeLevelRepository,
                                                geoCoordinatesRepository,
                                                countryRepository,
                                                administrativeLevelServiceAuditable,
                                                messageService);
    }

    @Bean
    public DistrictServiceValidator districtServiceValidator(MessageService messageService) {
        return new DistrictServiceValidatorImpl(messageService);
    }

    @Bean
    public DistrictService districtService(DistrictServiceValidator districtServiceValidator,
                                          DistrictRepository districtRepository,
                                          ProvinceRepository provinceRepository,
                                          GeoCoordinatesRepository geoCoordinatesRepository,
                                          AdministrativeLevelRepository administrativeLevelRepository,
                                          DistrictServiceAuditable districtServiceAuditable,
                                          MessageService messageService,
                                          ModelMapper modelMapper,
                                          LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService) {
        return new DistrictServiceImpl(districtServiceValidator,
                                      districtRepository,
                                      provinceRepository,
                                      geoCoordinatesRepository,
                                      administrativeLevelRepository,
                                      districtServiceAuditable,
                                      locationHierarchyCascadeSoftDeleteService,
                                      messageService,
                                      modelMapper);
    }

    @Bean
    public GeoCoordinatesServiceValidator geoCoordinatesServiceValidator(MessageService messageService) {
        return new GeoCoordinatesServiceValidatorImpl(messageService);
    }

    @Bean
    public GeoCoordinatesService geoCoordinatesService(GeoCoordinatesServiceValidator geoCoordinatesServiceValidator,
                                                     GeoCoordinatesRepository geoCoordinatesRepository,
                                                     GeoCoordinatesServiceAuditable geoCoordinatesServiceAuditable,
                                                     MessageService messageService,
                                                     ModelMapper modelMapper) {
        return new GeoCoordinatesServiceImpl(geoCoordinatesServiceValidator,
                                           geoCoordinatesRepository,
                                           geoCoordinatesServiceAuditable,
                                           messageService,
                                           modelMapper);
    }

    @Bean
    public LanguageServiceValidator languageServiceValidator(MessageService messageService) {
        return new LanguageServiceValidatorImpl(messageService);
    }

    @Bean
    public LanguageService languageService(LanguageServiceValidator languageServiceValidator,
                                         LanguageRepository languageRepository,
                                         LanguageServiceAuditable languageServiceAuditable,
                                         MessageService messageService,
                                         ModelMapper modelMapper) {
        return new LanguageServiceImpl(languageServiceValidator,
                                     languageRepository,
                                     languageServiceAuditable,
                                     messageService,
                                     modelMapper);
    }

    @Bean
    public LocalizedNameServiceValidator localizedNameServiceValidator(MessageService messageService) {
        return new LocalizedNameServiceValidatorImpl(messageService);
    }

    @Bean
    public LocalizedNameService localizedNameService(LocalizedNameServiceValidator localizedNameServiceValidator,
                                                   LocalizedNameRepository localizedNameRepository,
                                                   LanguageRepository languageRepository,
                                                   CountryRepository countryRepository,
                                                   ProvinceRepository provinceRepository,
                                                   DistrictRepository districtRepository,
                                                   SuburbRepository suburbRepository,
                                                   LocalizedNameServiceAuditable localizedNameServiceAuditable,
                                                   MessageService messageService,
                                                   ModelMapper modelMapper) {
        return new LocalizedNameServiceImpl(localizedNameServiceValidator,
                                          localizedNameRepository,
                                          languageRepository,
                                          countryRepository,
                                          provinceRepository,
                                          districtRepository,
                                          suburbRepository,
                                          localizedNameServiceAuditable,
                                          messageService,
                                          modelMapper);
    }

    @Bean
    public ProvinceServiceValidator provinceServiceValidator(MessageService messageService) {
        return new ProvinceServiceValidatorImpl(messageService);
    }

    @Bean
    public ProvinceService provinceService(ProvinceServiceValidator provinceServiceValidator,
                                         ProvinceRepository provinceRepository,
                                         CountryRepository countryRepository,
                                         GeoCoordinatesRepository geoCoordinatesRepository,
                                         AdministrativeLevelRepository administrativeLevelRepository,
                                         AdministrativeLevelServiceAuditable administrativeLevelServiceAuditable,
                                         LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService,
                                         ProvinceServiceAuditable provinceServiceAuditable,
                                         MessageService messageService,
                                         ModelMapper modelMapper) {
        return new ProvinceServiceImpl(provinceServiceValidator,
                                     provinceRepository,
                                     countryRepository,
                                     geoCoordinatesRepository,
                                     administrativeLevelRepository,
                                     administrativeLevelServiceAuditable,
                                     locationHierarchyCascadeSoftDeleteService,
                                     provinceServiceAuditable,
                                     messageService,
                                     modelMapper);
    }

    @Bean
    public SuburbServiceValidator suburbServiceValidator(MessageService messageService) {
        return new SuburbServiceValidatorImpl(messageService);
    }

    @Bean
    public SuburbService suburbService(SuburbServiceValidator suburbServiceValidator,
                                     SuburbRepository suburbRepository,
                                     DistrictRepository districtRepository,
                                     GeoCoordinatesRepository geoCoordinatesRepository,
                                     AdministrativeLevelRepository administrativeLevelRepository,
                                     SuburbServiceAuditable suburbServiceAuditable,
                                     LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService,
                                     MessageService messageService,
                                     ModelMapper modelMapper) {
        return new SuburbServiceImpl(suburbServiceValidator,
                                   suburbRepository,
                                   districtRepository,
                                   geoCoordinatesRepository,
                                   administrativeLevelRepository,
                                   suburbServiceAuditable,
                                   locationHierarchyCascadeSoftDeleteService,
                                   messageService,
                                   modelMapper);
    }

    @Bean
    public LocationNodeServiceValidator locationNodeServiceValidator() {
        return new LocationNodeServiceValidatorImpl();
    }

    @Bean
    public LocationNodeService locationNodeService(LocationNodeServiceValidator locationNodeServiceValidator,
                                                   LocationNodeRepository locationNodeRepository,
                                                   DistrictRepository districtRepository,
                                                   SuburbRepository suburbRepository,
                                                   LocationNodeServiceAuditable locationNodeServiceAuditable,
                                                   LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService,
                                                   org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate) {
        return new LocationNodeServiceImpl(
                locationNodeServiceValidator,
                locationNodeRepository,
                districtRepository,
                suburbRepository,
                locationNodeServiceAuditable,
                locationHierarchyCascadeSoftDeleteService,
                rabbitTemplate);
    }

    @Bean
    public CityServiceAuditable cityServiceAuditable(CityRepository cityRepository) {
        return new CityServiceAuditableImpl(cityRepository);
    }

    @Bean
    public CityServiceValidator cityServiceValidator(MessageService messageService) {
        return new CityServiceValidatorImpl(messageService);
    }

    @Bean
    public CityService cityService(CityServiceValidator cityServiceValidator,
                                    CityRepository cityRepository,
                                    DistrictRepository districtRepository,
                                    CityServiceAuditable cityServiceAuditable,
                                    MessageService messageService,
                                    LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService) {
        return new CityServiceImpl(
                cityServiceValidator,
                cityRepository,
                districtRepository,
                cityServiceAuditable,
                locationHierarchyCascadeSoftDeleteService,
                messageService);
    }

    @Bean
    public VillageServiceAuditable villageServiceAuditable(VillageRepository villageRepository) {
        return new VillageServiceAuditableImpl(villageRepository);
    }

    @Bean
    public VillageServiceValidator villageServiceValidator(MessageService messageService) {
        return new VillageServiceValidatorImpl(messageService);
    }

    @Bean
    public VillageService villageService(VillageServiceValidator villageServiceValidator,
                                         VillageRepository villageRepository,
                                         CityRepository cityRepository,
                                         DistrictRepository districtRepository,
                                         SuburbRepository suburbRepository,
                                         VillageServiceAuditable villageServiceAuditable,
                                         MessageService messageService,
                                         LocationHierarchyCascadeSoftDeleteService locationHierarchyCascadeSoftDeleteService) {
        return new VillageServiceImpl(
                villageServiceValidator,
                villageRepository,
                cityRepository,
                districtRepository,
                suburbRepository,
                villageServiceAuditable,
                locationHierarchyCascadeSoftDeleteService,
                messageService);
    }
}
