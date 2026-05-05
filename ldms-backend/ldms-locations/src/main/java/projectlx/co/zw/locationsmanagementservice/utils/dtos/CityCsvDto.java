package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * CSV row for {@code POST …/city/import-csv}. Headers are matched case-insensitively (OpenCSV).
 * Aligns with sample/export: NAME, CODE, DISTRICT ID, LATITUDE, LONGITUDE, TIMEZONE, POSTAL CODE.
 */
@Data
public class CityCsvDto {

    @CsvBindByName(column = "NAME", required = true)
    private String name;

    @CsvBindByName(column = "CODE")
    private String code;

    @CsvBindByName(column = "DISTRICT ID", required = true)
    private Long districtId;

    @CsvBindByName(column = "LATITUDE")
    private BigDecimal latitude;

    @CsvBindByName(column = "LONGITUDE")
    private BigDecimal longitude;

    @CsvBindByName(column = "TIMEZONE")
    private String timezone;

    @CsvBindByName(column = "POSTAL CODE")
    private String postalCode;
}
