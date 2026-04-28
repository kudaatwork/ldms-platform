package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

/**
 * DTO for mapping CSV data to Province.
 * Column headers must match exactly (case-insensitive) in the uploaded file.
 */
@Data
public class ProvinceCsvDto {

    @CsvBindByName(column = "NAME", required = true)
    private String name;

    @CsvBindByName(column = "CODE")
    private String code;

    @CsvBindByName(column = "COUNTRY ID", required = true)
    private Long countryId;

    @CsvBindByName(column = "ADMINISTRATIVE LEVEL ID")
    private Long administrativeLevelId;
}
