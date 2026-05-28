package projectlx.co.zw.organizationmanagement.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class IndustryCsvDto {

    @CsvBindByName(column = "NAME")
    private String name;

    @CsvBindByName(column = "INDUSTRY CODE")
    private String industryCode;

    @CsvBindByName(column = "DESCRIPTION")
    private String description;

    @CsvBindByName(column = "REGULATORY BODY NAME")
    private String regulatoryBodyName;

    @CsvBindByName(column = "REGULATORY BODY CONTACT")
    private String regulatoryBodyContactInfo;

    @CsvBindByName(column = "COMPLIANCE REQUIREMENTS")
    private String complianceRequirements;

    @CsvBindByName(column = "ACTIVE")
    private String active;
}
