package projectlx.co.zw.organizationmanagement.utils.dtos;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class AgentCsvDto {

    @CsvBindByName(column = "ORGANIZATION ID")
    private String organizationId;

    @CsvBindByName(column = "AGENT KIND")
    private String agentKind;

    @CsvBindByName(column = "FIRST NAME")
    private String firstName;

    @CsvBindByName(column = "LAST NAME")
    private String lastName;

    @CsvBindByName(column = "EMAIL")
    private String email;

    @CsvBindByName(column = "PHONE NUMBER")
    private String phoneNumber;

    @CsvBindByName(column = "AGENT TYPE")
    private String agentType;

    @CsvBindByName(column = "ROLE")
    private String role;

    @CsvBindByName(column = "BRANCH ID")
    private String branchId;

    @CsvBindByName(column = "ACTIVE")
    private String active;
}
