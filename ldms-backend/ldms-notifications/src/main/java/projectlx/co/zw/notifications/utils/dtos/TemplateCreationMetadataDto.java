package projectlx.co.zw.notifications.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Metadata for rendering the Add Template form in a stepped, intuitive way (like Add Organization).")
public class TemplateCreationMetadataDto {

    @Schema(description = "Form sections in display order. Each section maps to a step or accordion.")
    private List<TemplateFormSectionDto> sections;

    @Schema(description = "Available channel options for the channel multi-select.")
    private List<ChannelOptionDto> channelOptions;

    public TemplateCreationMetadataDto() {
    }

    public TemplateCreationMetadataDto(List<TemplateFormSectionDto> sections, List<ChannelOptionDto> channelOptions) {
        this.sections = sections;
        this.channelOptions = channelOptions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<TemplateFormSectionDto> getSections() {
        return sections;
    }

    public void setSections(List<TemplateFormSectionDto> sections) {
        this.sections = sections;
    }

    public List<ChannelOptionDto> getChannelOptions() {
        return channelOptions;
    }

    public void setChannelOptions(List<ChannelOptionDto> channelOptions) {
        this.channelOptions = channelOptions;
    }

    public static final class Builder {
        private List<TemplateFormSectionDto> sections;
        private List<ChannelOptionDto> channelOptions;

        public Builder sections(List<TemplateFormSectionDto> sections) {
            this.sections = sections;
            return this;
        }

        public Builder channelOptions(List<ChannelOptionDto> channelOptions) {
            this.channelOptions = channelOptions;
            return this;
        }

        public TemplateCreationMetadataDto build() {
            return new TemplateCreationMetadataDto(sections, channelOptions);
        }
    }
}
