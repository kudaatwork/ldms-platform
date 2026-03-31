package projectlx.co.zw.locationsmanagementservice.service.processor.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocationIngestionService;
import projectlx.co.zw.locationsmanagementservice.business.logic.api.LocationIngestionService.GooglePlaceDetails;
import projectlx.co.zw.locationsmanagementservice.model.Address;
import projectlx.co.zw.locationsmanagementservice.service.processor.api.LocationIngestionProcessor;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LocationIngestionProcessorImpl implements LocationIngestionProcessor {

    private final LocationIngestionService locationIngestionService;
    private final Logger logger = LoggerFactory.getLogger(LocationIngestionProcessorImpl.class);

    @Override
    public Address ingestGooglePlace(GooglePlaceDetails details, Locale locale, String username) {
        logger.info("Incoming request to ingest Google Place: {}", details);

        Address address = locationIngestionService.ingestGooglePlace(details, locale, username);

        logger.info("Outgoing response after ingesting Google Place. Address ID: {}", 
                address != null ? address.getId() : "null");

        return address;
    }
}