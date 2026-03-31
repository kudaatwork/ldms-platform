package projectlx.co.zw.notificationsmanagementservice.utils.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.co.zw.notificationsmanagementservice.model.Channel;

import java.util.List;

@Getter
@Setter
@ToString
@Schema(description = "Request to create a new notification template. Structure mirrors a stepped form: Identity → Channels → per-channel content.")
public class CreateTemplateRequest {

    // === SECTION: Template identity (Step 1 – same as “Basic info” in Add Organization) ===
    @NotEmpty(message = "Template key cannot be empty")
    @Schema(description = "Unique key used by the system to trigger this template (e.g. ORGANIZATION_SUPPLIER_APPROVED). UPPER_SNAKE_CASE recommended.", example = "ORDER_CONFIRMATION", requiredMode = Schema.RequiredMode.REQUIRED)
    private String templateKey;

    @NotEmpty(message = "Description cannot be empty")
    @Schema(description = "Short description of when this template is used, for admin reference.", example = "Sent when an organization is approved by admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    // === SECTION: Channel selection (Step 2) ===
    @NotNull(message = "Channels list cannot be null")
    @Schema(description = "Delivery channels for this template. At least one required. Content fields below are validated only for selected channels.", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Channel> channels;

    // === SECTION: Email content (shown when EMAIL is in channels) ===
    @Schema(description = "Email subject line. Required when EMAIL is selected in channels.")
    private String emailSubject;

    @Schema(description = "HTML body for email. Supports Handlebars placeholders e.g. {{organizationName}}. Required when EMAIL is selected.")
    private String emailBodyHtml;

    // === SECTION: SMS content (shown when SMS is in channels) ===
    @Schema(description = "SMS message body. Max 320 characters. Required when SMS is selected in channels.")
    private String smsBody;

    // === SECTION: In-app content (shown when IN_APP is in channels) ===
    @Schema(description = "Title for in-app notification. Required when IN_APP is selected in channels.")
    private String inAppTitle;

    @Schema(description = "Body text for in-app notification. Required when IN_APP is selected in channels.")
    private String inAppBody;

    // === SECTION: WhatsApp (shown when WHATSAPP is in channels) ===
    @Schema(description = "Twilio/WhatsApp template name (Content SID). Required when WHATSAPP is selected in channels.")
    private String whatsappTemplateName;
}
