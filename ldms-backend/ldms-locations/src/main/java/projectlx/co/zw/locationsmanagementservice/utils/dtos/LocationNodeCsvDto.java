package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LocationNodeCsvDto {

    @CsvBindByName(column = "NAME", required = true)
    private String name;

    @CsvBindByName(column = "CODE")
    private String code;

    @CsvBindByName(column = "LOCATION TYPE", required = true)
    private String locationType;

    @CsvBindByName(column = "PARENT ID")
    private Long parentId;

    @CsvBindByName(column = "DISTRICT ID")
    private Long districtId;

    @CsvBindByName(column = "SUBURB ID")
    private Long suburbId;

    @CsvBindByName(column = "LATITUDE")
    private BigDecimal latitude;

    @CsvBindByName(column = "LONGITUDE")
    private BigDecimal longitude;

    @CsvBindByName(column = "TIMEZONE")
    private String timezone;

    @CsvBindByName(column = "POSTAL CODE")
    private String postalCode;

    @CsvBindByName(column = "ALIASES")
    private String aliases;
}
