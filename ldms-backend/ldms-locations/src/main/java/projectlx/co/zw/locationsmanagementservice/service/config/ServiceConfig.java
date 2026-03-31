package projectlx.co.zw.locationsmanagementservice.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.AddressService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.AdministrativeLevelService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.CountryService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.DistrictService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.GeoCoordinatesService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LanguageService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocationIngestionService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocalizedNameService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocationNodeService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.ProvinceService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.SuburbService;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.AddressServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.AdministrativeLevelServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.CountryServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.DistrictServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.GeoCoordinatesServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LanguageServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LocationIngestionProcessor;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LocalizedNameServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LocationNodeServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.ProvinceServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.SuburbServiceProcessor;
import projectlx.co.zw.locationsmanagementservice.service.processor.impl.AddressServiceProcessorImpl;
import projectlx.co.zw.locationsmanagementservice.service.processor.impl.AdministrativeLevelServiceProcessorImpl;
import projectlx.co.zw.locationsmanagementservice.service.processor.impl.CountryServiceProcessorImpl;
import projectlx.co.zw.locationsmanagementservice.service.processor.impl.DistrictServiceProcessorImpl;
import projectlx.co.zw.locationsmanagementservice.service.processor.impl.GeoCoordinatesServiceProcessorImpl;
import projectlx.co.zw.locationsmanagementservice.service.processor.impl.LanguageServiceProcessorImpl;
import projectlx.co.zw.locationsmanagementservice.service.processor.impl.LocationIngestionProcessorImpl;
import projectlx.co.zw.locationsmanagementservice.service.processor.impl.LocalizedNameServiceProcessorImpl;
import projectlx.co.zw.locationsmanagementservice.service.processor.impl.LocationNodeServiceProcessorImpl;
import projectlx.co.zw.locationsmanagementservice.service.processor.impl.ProvinceServiceProcessorImpl;
import projectlx.co.zw.locationsmanagementservice.service.processor.impl.SuburbServiceProcessorImpl;

@Configuration
public class ServiceConfig
{

    @Bean
    public CountryServiceProcessor countryServiceProcessor(CountryService countryService) {
        return new CountryServiceProcessorImpl(countryService);
    }

    @Bean
    public AddressServiceProcessor addressServiceProcessor(AddressService addressService) {
        return new AddressServiceProcessorImpl(addressService);
    }

    @Bean
    public AdministrativeLevelServiceProcessor administrativeLevelServiceProcessor(AdministrativeLevelService administrativeLevelService) {
        return new AdministrativeLevelServiceProcessorImpl(administrativeLevelService);
    }

    @Bean
    public DistrictServiceProcessor districtServiceProcessor(DistrictService districtService) {
        return new DistrictServiceProcessorImpl(districtService);
    }

    @Bean
    public GeoCoordinatesServiceProcessor geoCoordinatesServiceProcessor(GeoCoordinatesService geoCoordinatesService) {
        return new GeoCoordinatesServiceProcessorImpl(geoCoordinatesService);
    }

    @Bean
    public LanguageServiceProcessor languageServiceProcessor(LanguageService languageService) {
        return new LanguageServiceProcessorImpl(languageService);
    }

    @Bean
    public LocalizedNameServiceProcessor localizedNameServiceProcessor(LocalizedNameService localizedNameService) {
        return new LocalizedNameServiceProcessorImpl(localizedNameService);
    }

    @Bean
    public ProvinceServiceProcessor provinceServiceProcessor(ProvinceService provinceService) {
        return new ProvinceServiceProcessorImpl(provinceService);
    }

    @Bean
    public SuburbServiceProcessor suburbServiceProcessor(SuburbService suburbService) {
        return new SuburbServiceProcessorImpl(suburbService);
    }

    @Bean
    public LocationIngestionProcessor locationIngestionProcessor(LocationIngestionService locationIngestionService) {
        return new LocationIngestionProcessorImpl(locationIngestionService);
    }

    @Bean
    public LocationNodeServiceProcessor locationNodeServiceProcessor(LocationNodeService locationNodeService) {
        return new LocationNodeServiceProcessorImpl(locationNodeService);
    }
}
