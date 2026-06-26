package projectlx.messaging.inbound.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import projectlx.co.zw.shared_library.utils.enums.EntityStatus;
import projectlx.messaging.inbound.utils.enums.BotAssistantMode;
import projectlx.messaging.inbound.utils.enums.BotChannel;
import projectlx.messaging.inbound.utils.enums.BotSessionStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "bot_session")
public class BotSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 32)
    private String sessionId;

    @Column(name = "requester_username", nullable = false, length = 150)
    private String requesterUsername;

    @Column(name = "user_display_name", nullable = false, length = 200)
    private String userDisplayName;

    @Column(name = "user_phone", length = 40)
    private String userPhone;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "organization_name", length = 200)
    private String organizationName;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50)
    private BotChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BotSessionStatus status;

    @Column(name = "topic", length = 200)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "assistant_mode", nullable = false, length = 50)
    private BotAssistantMode assistantMode;

    @Column(name = "satisfaction_score")
    private Integer satisfactionScore;

    @Column(name = "escalated_ticket_id")
    private Long escalatedTicketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_status", nullable = false, length = 50)
    private EntityStatus entityStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by", length = 150)
    private String modifiedBy;
}
