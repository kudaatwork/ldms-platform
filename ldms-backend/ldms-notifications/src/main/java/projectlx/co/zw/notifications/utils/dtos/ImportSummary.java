package projectlx.co.zw.notifications.utils.dtos;

import lombok.RequiredArgsConstructor;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import java.util.List;

@RequiredArgsConstructor
public class ImportSummary extends CommonResponse {
    public int total;
    public int success;
    public int failed;
    private List<String> errorMessages;

    public ImportSummary(int total, int success, int failed, List<String> errorMessages) {
        this.total = total;
        this.success = success;
        this.failed = failed;
        this.errorMessages = errorMessages;
    }

    public ImportSummary(int statusCode, boolean isSuccess, String message, int total, int success, int failed, List<String> errorMessages) {
        this.setStatusCode(statusCode);
        this.setSuccess(isSuccess);
        this.setMessage(message);
        this.total = total;
        this.success = success;
        this.failed = failed;
        this.errorMessages = errorMessages;
    }
}