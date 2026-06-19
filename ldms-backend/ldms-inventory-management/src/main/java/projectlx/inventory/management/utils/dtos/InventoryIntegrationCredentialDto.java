package projectlx.inventory.management.utils.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.inventory.management.model.IntegrationCredentialStatus;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryIntegrationCredentialDto {

    private Long id;
    private Long organizationId;
    private String credentialLabel;
    private String apiKey;
    private String webhookUrl;
    private String callbackGrvUrl;
    private IntegrationCredentialStatus status;
    private LocalDateTime lastUsedAt;
    private EntityStatus entityStatus;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
}
