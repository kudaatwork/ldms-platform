package projectlx.co.zw.notificationsmanagementservice.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Channel option for the Add Template channel selector.")
public class ChannelOptionDto {
    @Schema(description = "Channel enum value", example = "EMAIL")
    private String value;
    @Schema(description = "Display label", example = "Email")
    private String label;
    @Schema(description = "Short description for the channel")
    private String description;
}
