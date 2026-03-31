package projectlx.co.zw.notifications.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "One section of the Add Template form (e.g. Identity, Channels, Email content). Renders as a step or accordion like Add Organization.")
public class TemplateFormSectionDto {
    @Schema(description = "Unique section key for conditional display", example = "identity")
    private String sectionKey;
    @Schema(description = "Display label for the section", example = "Template identity")
    private String sectionLabel;
    @Schema(description = "Short hint for the section")
    private String sectionDescription;
    @Schema(description = "Display order (1-based)")
    private int order;
    @Schema(description = "Request field names belonging to this section (e.g. templateKey, description)")
    private List<String> fieldKeys;
}
