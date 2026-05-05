package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * CSV row for {@code POST …/village/import-csv}. Headers matched case-insensitively (OpenCSV).
 * Aligns with sample/export: NAME, CODE, CITY ID, DISTRICT ID, SUBURB ID (optional), LATITUDE, LONGITUDE,
 * TIMEZONE, POSTAL CODE.
 */
@Data
public class VillageCsvDto {

    @CsvBindByName(column = "NAME", required = true)
    private String name;

    @CsvBindByName(column = "CODE")
    private String code;

    @CsvBindByName(column = "CITY ID", required = true)
    private Long cityId;

    @CsvBindByName(column = "DISTRICT ID", required = true)
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
}
