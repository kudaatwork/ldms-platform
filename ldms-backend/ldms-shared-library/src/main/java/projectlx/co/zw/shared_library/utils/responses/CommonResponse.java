package projectlx.co.zw.shared_library.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse {
    private int statusCode;
    private boolean isSuccess;
    private String message;
    private List<String> errorMessages;

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public boolean isSuccess() { return isSuccess; }
    public void setSuccess(boolean isSuccess) { this.isSuccess = isSuccess; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getErrorMessages() { return errorMessages; }
    public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }
}
