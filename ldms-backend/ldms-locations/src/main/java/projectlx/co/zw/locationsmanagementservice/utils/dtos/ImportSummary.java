package projectlx.co.zw.locationsmanagementservice.utils.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

/**
 * CSV import outcome. The success row count uses {@link #importedCount} / JSON {@code importedCount}
 * so it does not clash with {@link CommonResponse#isSuccess}, which Jackson exposes as {@code success}.
 */
@NoArgsConstructor
public class ImportSummary extends CommonResponse {

    public int total;

    @JsonProperty("importedCount")
    public int importedCount;

    public int failed;

    public ImportSummary(int total, int importedCount, int failed, List<String> errorMessages) {
        this.total = total;
        this.importedCount = importedCount;
        this.failed = failed;
        this.setErrorMessages(errorMessages);
    }

    public ImportSummary(int statusCode, boolean isSuccess, String message, int total, int importedCount, int failed,
            List<String> errorMessages) {
        this.setStatusCode(statusCode);
        this.setSuccess(isSuccess);
        this.setMessage(message);
        this.total = total;
        this.importedCount = importedCount;
        this.failed = failed;
        this.setErrorMessages(errorMessages);
    }
}
