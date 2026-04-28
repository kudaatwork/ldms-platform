package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

/**
 * DTO for mapping CSV data to Address.
 * Column headers must match exactly (case-insensitive) in the uploaded file.
 */
@Data
public class AddressCsvDto {

    @CsvBindByName(column = "LINE 1", required = true)
    private String line1;

    @CsvBindByName(column = "LINE 2")
    private String line2;

    @CsvBindByName(column = "POSTAL CODE")
    private String postalCode;

    @CsvBindByName(column = "SUBURB ID")
    private Long suburbId;
}
