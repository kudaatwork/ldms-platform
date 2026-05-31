package projectlx.user.management.utils.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.user.management.model.SupportTicketCategory;
import projectlx.user.management.model.SupportTicketPriority;

@Getter
@Setter
@ToString
public class CreateSupportTicketRequest {

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Short summary of the issue")
    private String subject;

    @NotBlank
    @Size(min = 20, max = 8000)
    @Schema(description = "Detailed description including references (PO, shipment, invoice, etc.)")
    private String description;

    @NotNull
    @Schema(description = "Ticket category")
    private SupportTicketCategory category;

    @Schema(description = "Optional priority override; defaults to NORMAL")
    private SupportTicketPriority priority;
}
