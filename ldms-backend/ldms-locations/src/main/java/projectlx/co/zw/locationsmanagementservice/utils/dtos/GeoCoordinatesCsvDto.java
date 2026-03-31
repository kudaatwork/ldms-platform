package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for mapping CSV data to GeoCoordinates
 */
@Data
public class GeoCoordinatesCsvDto {
    
    @CsvBindByName(column = "LATITUDE")
    private BigDecimal latitude;
    
    @CsvBindByName(column = "LONGITUDE")
    private BigDecimal longitude;
}