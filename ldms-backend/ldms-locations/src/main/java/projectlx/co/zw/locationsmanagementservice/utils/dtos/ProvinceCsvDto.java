package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for mapping CSV data to Province.
 * Column headers must match exactly (case-insensitive) in the uploaded file.
 * <p>Import resolves {@code ADMINISTRATIVE LEVEL ID} when it is blank, zero, unknown, or not for
 * {@code COUNTRY ID}: prefers that country's active level with {@code level == 1}, else the active level
 * with the smallest {@code level} value {@code >= 1}; if none exist, a tier-1 level is auto-created for
 * that country when possible.</p>
 * <p>Geo coordinates: optional {@code LATITUDE}/{@code LONGITUDE} or {@code GEO COORDINATES ID}. When all
 * are omitted, coordinates are copied from the parent country's geo row (new {@link projectlx.co.zw.locationsmanagementservice.model.GeoCoordinates}
 * is persisted for the province), matching the admin UI sample CSV description.</p>
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

    @CsvBindByName(column = "LATITUDE")
    private BigDecimal latitude;

    @CsvBindByName(column = "LONGITUDE")
    private BigDecimal longitude;

    @CsvBindByName(column = "GEO COORDINATES ID")
    private Long geoCoordinatesId;
}
