package projectlx.co.zw.shared_library.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse {
    private int statusCode;
    private boolean isSuccess;
    private String message;
    private List<String> errorMessages;
}
