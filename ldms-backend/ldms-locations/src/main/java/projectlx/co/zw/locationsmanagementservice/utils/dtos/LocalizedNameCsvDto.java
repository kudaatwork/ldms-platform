package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

/**
 * CSV row for {@code POST …/localized-name/import-csv}. Headers must match (case-insensitive).
 */
@Data
public class LocalizedNameCsvDto {

    @CsvBindByName(column = "VALUE", required = true)
    private String value;

    @CsvBindByName(column = "LANGUAGE ID", required = true)
    private String languageId;

    @CsvBindByName(column = "REFERENCE TYPE", required = true)
    private String referenceType;

    @CsvBindByName(column = "REFERENCE ID", required = true)
    private String referenceId;
}
