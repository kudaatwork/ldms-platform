package projectlx.user.management.utils.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.shared_library.utils.responses.CommonResponse;
import projectlx.user.management.utils.dtos.DemoRequisitionDto;
import projectlx.user.management.utils.dtos.HelpArticleDto;
import projectlx.user.management.utils.dtos.HelpPlatformStatusDto;
import projectlx.user.management.utils.dtos.SupportTicketDto;

import java.util.List;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HelpSupportResponse extends CommonResponse {
    private HelpArticleDto helpArticleDto;
    private List<HelpArticleDto> helpArticleDtoList;
    private SupportTicketDto supportTicketDto;
    private List<SupportTicketDto> supportTicketDtoList;
    private HelpPlatformStatusDto platformStatusDto;
    private DemoRequisitionDto demoRequisitionDto;
    private List<DemoRequisitionDto> demoRequisitionDtoList;
}
