package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for mapping CSV data to Suburb.
 * Column headers must match exactly (case-insensitive) in the uploaded file.
 */
@Data
public class SuburbCsvDto {

    @CsvBindByName(column = "NAME", required = true)
    private String name;

    @CsvBindByName(column = "CODE")
    private String code;

    @CsvBindByName(column = "POSTAL CODE")
    private String postalCode;

    @CsvBindByName(column = "DISTRICT ID", required = true)
    private Long districtId;

    @CsvBindByName(column = "ADMINISTRATIVE LEVEL ID")
    private Long administrativeLevelId;

    @CsvBindByName(column = "LATITUDE")
    private BigDecimal latitude;

    @CsvBindByName(column = "LONGITUDE")
    private BigDecimal longitude;

    @CsvBindByName(column = "GEO COORDINATES ID")
    private Long geoCoordinatesId;
}
