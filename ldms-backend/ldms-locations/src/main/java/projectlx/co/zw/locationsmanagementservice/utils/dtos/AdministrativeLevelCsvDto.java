package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

/**
 * DTO for mapping CSV data to AdministrativeLevel.
 * Column headers must match exactly (case-insensitive) in the uploaded file.
 */
@Data
public class AdministrativeLevelCsvDto {

    @CsvBindByName(column = "NAME", required = true)
    private String name;

    @CsvBindByName(column = "CODE")
    private String code;

    @CsvBindByName(column = "LEVEL", required = true)
    private Integer level;

    @CsvBindByName(column = "DESCRIPTION")
    private String description;
}
