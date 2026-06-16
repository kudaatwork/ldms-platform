package projectlx.co.zw.organizationmanagement.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class BranchCsvDto {

    @CsvBindByName(column = "ORGANIZATION ID")
    private String organizationId;

    @CsvBindByName(column = "BRANCH NAME")
    private String branchName;

    @CsvBindByName(column = "BRANCH CODE")
    private String branchCode;

    @CsvBindByName(column = "REGION")
    private String region;

    @CsvBindByName(column = "EMAIL")
    private String email;

    @CsvBindByName(column = "PHONE NUMBER")
    private String phoneNumber;

    @CsvBindByName(column = "HEAD OFFICE")
    private String headOffice;

    @CsvBindByName(column = "ACTIVE")
    private String active;

    @CsvBindByName(column = "BUSINESS HOURS")
    private String businessHours;

    @CsvBindByName(column = "LOCATION ID")
    private String locationId;

    @CsvBindByName(column = "PARENT BRANCH ID")
    private String parentBranchId;

    @CsvBindByName(column = "DEPOT")
    private String depot;
}
