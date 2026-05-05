package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByNames;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for mapping CSV data to Country.
 * Headers accept common export variants (hyphen vs word spacing).
 * Optional {@code LATITUDE} / {@code LONGITUDE}: when both are set, the server saves a new geo row and links it.
 */
@Data
public class CountryCsvDto {

    @CsvBindByName(column = "NAME")
    private String name;

    @CsvBindByNames(value = {
            @CsvBindByName(column = "ISO ALPHA-2 CODE"),
            @CsvBindByName(column = "ISO-ALPHA-2")
    })
    private String isoAlpha2Code;

    @CsvBindByNames(value = {
            @CsvBindByName(column = "ISO ALPHA-3 CODE"),
            @CsvBindByName(column = "ISO-ALPHA-3")
    })
    private String isoAlpha3Code;

    @CsvBindByNames(value = {
            @CsvBindByName(column = "DIAL CODE"),
            @CsvBindByName(column = "DIAL-CODE")
    })
    private String dialCode;

    @CsvBindByName(column = "TIMEZONE")
    private String timezone;

    @CsvBindByNames(value = {
            @CsvBindByName(column = "CURRENCY CODE"),
            @CsvBindByName(column = "CURRENCY")
    })
    private String currencyCode;

    @CsvBindByNames(value = {
            @CsvBindByName(column = "GEOCOORDINATES ID"),
            @CsvBindByName(column = "GEO COORDINATES ID"),
            @CsvBindByName(column = "GEOCOORDID"),
            @CsvBindByName(column = "GEO COORD ID")
    })
    private Long geoCoordinatesId;

    @CsvBindByName(column = "LATITUDE")
    private BigDecimal latitude;

    @CsvBindByName(column = "LONGITUDE")
    private BigDecimal longitude;
}
