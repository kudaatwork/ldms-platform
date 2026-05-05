package projectlx.co.zw.audittrail.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.domain.Page;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogChurnHistoryDto;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogChurnLaunchDto;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogChurnOutDto;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogDto;
import projectlx.co.zw.audittrail.utils.dtos.AuditLogServiceStats;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponse extends CommonResponse {

    private AuditLogDto auditLog;
    private List<AuditLogDto> auditLogList;
    private Page<AuditLogDto> auditLogPage;
    private AuditLogServiceStats serviceStats;
    private AuditLogChurnOutDto churnOut;
    /** Present when churn is executed via Spring Batch (async launch). */
    private AuditLogChurnLaunchDto churnLaunch;
    /** Paged list for the churn-out history table. */
    private Page<AuditLogChurnHistoryDto> churnHistoryPage;
}
