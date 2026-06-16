package projectlx.co.zw.notifications.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

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

    public TemplateFormSectionDto() {
    }

    public TemplateFormSectionDto(
            String sectionKey,
            String sectionLabel,
            String sectionDescription,
            int order,
            List<String> fieldKeys) {
        this.sectionKey = sectionKey;
        this.sectionLabel = sectionLabel;
        this.sectionDescription = sectionDescription;
        this.order = order;
        this.fieldKeys = fieldKeys;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSectionKey() {
        return sectionKey;
    }

    public void setSectionKey(String sectionKey) {
        this.sectionKey = sectionKey;
    }

    public String getSectionLabel() {
        return sectionLabel;
    }

    public void setSectionLabel(String sectionLabel) {
        this.sectionLabel = sectionLabel;
    }

    public String getSectionDescription() {
        return sectionDescription;
    }

    public void setSectionDescription(String sectionDescription) {
        this.sectionDescription = sectionDescription;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<String> getFieldKeys() {
        return fieldKeys;
    }

    public void setFieldKeys(List<String> fieldKeys) {
        this.fieldKeys = fieldKeys;
    }

    public static final class Builder {
        private String sectionKey;
        private String sectionLabel;
        private String sectionDescription;
        private int order;
        private List<String> fieldKeys;

        public Builder sectionKey(String sectionKey) {
            this.sectionKey = sectionKey;
            return this;
        }

        public Builder sectionLabel(String sectionLabel) {
            this.sectionLabel = sectionLabel;
            return this;
        }

        public Builder sectionDescription(String sectionDescription) {
            this.sectionDescription = sectionDescription;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder fieldKeys(List<String> fieldKeys) {
            this.fieldKeys = fieldKeys;
            return this;
        }

        public TemplateFormSectionDto build() {
            return new TemplateFormSectionDto(sectionKey, sectionLabel, sectionDescription, order, fieldKeys);
        }
    }
}
