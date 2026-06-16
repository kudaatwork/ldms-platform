package projectlx.co.zw.notifications.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String templateKey;

    @Column(nullable = false)
    private String description;

    // Fixed: Changed from jsonb to json for MySQL compatibility
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private List<Channel> channels;

    /** When set, only channels mapped to {@code true} are dispatched (others may still appear in {@link #channels}). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "channel_delivery_enabled", columnDefinition = "json")
    private Map<String, Boolean> channelDeliveryEnabled;

    @Column(length = 255)
    private String emailSubject;

    @Lob
    private String emailBodyHtml;

    @Column(length = 320)
    private String smsBody;

    @Column(length = 150)
    private String inAppTitle;

    @Lob
    private String inAppBody;

    @Column(length = 255)
    private String whatsappTemplateName;

    @Column(name = "whatsapp_body", columnDefinition = "TEXT")
    private String whatsappBody;

    @Column(nullable = false)
    private boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    @PrePersist
    public void prePersist() {
        whatsappBody = normalizeNewLines(whatsappBody);
        createdAt    = LocalDateTime.now();
        updatedAt    = createdAt;
        entityStatus = EntityStatus.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {
        whatsappBody = normalizeNewLines(whatsappBody);
        updatedAt = LocalDateTime.now();
    }

    private String normalizeNewLines(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\n", "\n");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Channel> getChannels() { return channels; }
    public void setChannels(List<Channel> channels) { this.channels = channels; }

    public Map<String, Boolean> getChannelDeliveryEnabled() { return channelDeliveryEnabled; }
    public void setChannelDeliveryEnabled(Map<String, Boolean> channelDeliveryEnabled) { this.channelDeliveryEnabled = channelDeliveryEnabled; }

    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }

    public String getEmailBodyHtml() { return emailBodyHtml; }
    public void setEmailBodyHtml(String emailBodyHtml) { this.emailBodyHtml = emailBodyHtml; }

    public String getSmsBody() { return smsBody; }
    public void setSmsBody(String smsBody) { this.smsBody = smsBody; }

    public String getInAppTitle() { return inAppTitle; }
    public void setInAppTitle(String inAppTitle) { this.inAppTitle = inAppTitle; }

    public String getInAppBody() { return inAppBody; }
    public void setInAppBody(String inAppBody) { this.inAppBody = inAppBody; }

    public String getWhatsappTemplateName() { return whatsappTemplateName; }
    public void setWhatsappTemplateName(String whatsappTemplateName) { this.whatsappTemplateName = whatsappTemplateName; }

    public String getWhatsappBody() { return whatsappBody; }
    public void setWhatsappBody(String whatsappBody) { this.whatsappBody = whatsappBody; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public EntityStatus getEntityStatus() { return entityStatus; }
    public void setEntityStatus(EntityStatus entityStatus) { this.entityStatus = entityStatus; }

    @Override
    public String toString() {
        return "NotificationTemplate{id=" + id + ", templateKey='" + templateKey + "', channels=" + channels + "}";
    }
}