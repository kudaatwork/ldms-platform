package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

/**
 * DTO for mapping CSV data to Language.
 * Column headers must match exactly (case-insensitive) in the uploaded file.
 */
@Data
public class LanguageCsvDto {

    @CsvBindByName(column = "NAME", required = true)
    private String name;

    @CsvBindByName(column = "ISO CODE")
    private String isoCode;

    @CsvBindByName(column = "NATIVE NAME")
    private String nativeName;

    @CsvBindByName(column = "IS DEFAULT")
    private String isDefault;
}
