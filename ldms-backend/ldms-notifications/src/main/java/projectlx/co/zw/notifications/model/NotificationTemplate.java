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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table
@Getter
@Setter
@ToString
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

    @Column(nullable = false)
    private boolean isActive = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    @PrePersist
    public void prePersist() {
        createdAt    = LocalDateTime.now();
        updatedAt    = createdAt;
        entityStatus = EntityStatus.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}