package projectlx.user.management.utils.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

/**
 * Model class for EasyExcel to read user data from Excel files
 */
@Data
public class UserExcelModel {
    
    @ExcelProperty("Username")
    private String username;
    
    @ExcelProperty("Email")
    private String email;
    
    @ExcelProperty("First Name")
    private String firstName;
    
    @ExcelProperty("Last Name")
    private String lastName;
    
    @ExcelProperty("Phone Number")
    private String phoneNumber;
    
    @ExcelProperty("National ID Number")
    private String nationalIdNumber;
    
    @ExcelProperty("Passport Number")
    private String passportNumber;
    
    @ExcelProperty("Date of Birth")
    private Date dateOfBirth;
    
    @ExcelProperty("Gender")
    private String gender;
}