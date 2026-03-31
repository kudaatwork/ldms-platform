package projectlx.user.management.service.utils.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.Getter;
import projectlx.user.management.service.model.Gender;
import projectlx.user.management.service.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener class for EasyExcel to process user data from Excel files
 */
public class UserExcelListener extends AnalysisEventListener<UserExcelModel> {
    
    @Getter
    private final List<User> users = new ArrayList<>();
    
    @Override
    public void invoke(UserExcelModel userExcelModel, AnalysisContext analysisContext) {
        // Convert UserExcelModel to User
        User user = new User();
        user.setUsername(userExcelModel.getUsername());
        user.setEmail(userExcelModel.getEmail());
        user.setFirstName(userExcelModel.getFirstName());
        user.setLastName(userExcelModel.getLastName());
        user.setPhoneNumber(userExcelModel.getPhoneNumber());
        user.setNationalIdNumber(userExcelModel.getNationalIdNumber());
        user.setPassportNumber(userExcelModel.getPassportNumber());
        user.setDateOfBirth(userExcelModel.getDateOfBirth());
        
        // Convert gender string to Gender enum
        if (userExcelModel.getGender() != null && !userExcelModel.getGender().isEmpty()) {
            try {
                user.setGender(Gender.valueOf(userExcelModel.getGender().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Invalid gender value, leave as null
            }
        }
        
        users.add(user);
    }
    
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // This method is called after all data has been processed
    }
}