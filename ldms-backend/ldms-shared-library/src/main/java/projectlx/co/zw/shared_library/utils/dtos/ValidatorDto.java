package projectlx.co.zw.shared_library.utils.dtos;

import java.util.List;

public class ValidatorDto {

    public Boolean success;
    public String data;
    public List<String> errorMessages;

    public ValidatorDto() {
    }

    public ValidatorDto(Boolean success, String data, List<String> errorMessages) {
        this.success = success;
        this.data = data;
        this.errorMessages = errorMessages;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }
}
