package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for mapping CSV data to Country
 */
@Data
public class CountryCsvDto {
    
    @CsvBindByName(column = "NAME")
    private String name;
    
    @CsvBindByName(column = "ISO ALPHA-2 CODE")
    private String isoAlpha2Code;
    
    @CsvBindByName(column = "ISO ALPHA-3 CODE")
    private String isoAlpha3Code;
    
    @CsvBindByName(column = "DIAL CODE")
    private String dialCode;
    
    @CsvBindByName(column = "TIMEZONE")
    private String timezone;
    
    @CsvBindByName(column = "CURRENCY CODE")
    private String currencyCode;

    @CsvBindByName(column = "GEOCOORDINATES ID")
    private Long geoCoordinatesId;
}