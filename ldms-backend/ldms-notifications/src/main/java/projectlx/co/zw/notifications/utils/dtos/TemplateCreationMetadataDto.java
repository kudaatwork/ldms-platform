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
@Schema(description = "Metadata for rendering the Add Template form in a stepped, intuitive way (like Add Organization).")
public class TemplateCreationMetadataDto {
    @Schema(description = "Form sections in display order. Each section maps to a step or accordion.")
    private List<TemplateFormSectionDto> sections;
    @Schema(description = "Available channel options for the channel multi-select.")
    private List<ChannelOptionDto> channelOptions;
}
