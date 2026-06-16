package projectlx.co.zw.notifications.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Channel option for the Add Template channel selector.")
public class ChannelOptionDto {

    @Schema(description = "Channel enum value", example = "EMAIL")
    private String value;

    @Schema(description = "Display label", example = "Email")
    private String label;

    @Schema(description = "Short description for the channel")
    private String description;

    public ChannelOptionDto() {
    }

    public ChannelOptionDto(String value, String label, String description) {
        this.value = value;
        this.label = label;
        this.description = description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static final class Builder {
        private String value;
        private String label;
        private String description;

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public ChannelOptionDto build() {
            return new ChannelOptionDto(value, label, description);
        }
    }
}
