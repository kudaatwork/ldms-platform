package projectlx.co.zw.organizationmanagement.utils.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

import java.util.List;

/**
 * CSV import outcome aligned with locations {@code ImportSummary}.
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
        setErrorMessages(errorMessages);
    }

    public ImportSummary(int statusCode, boolean isSuccess, String message, int total, int importedCount, int failed,
                         List<String> errorMessages) {
        setStatusCode(statusCode);
        setSuccess(isSuccess);
        setMessage(message);
        this.total = total;
        this.importedCount = importedCount;
        this.failed = failed;
        setErrorMessages(errorMessages);
    }
}
