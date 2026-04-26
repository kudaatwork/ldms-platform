package projectlx.user.management.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@ToString
public class UserPreferences {
    // User Preferences Information
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for the user_preferences
    private String preferredLanguage; // User's preferred language
    private String timezone; // User's timezone
    private LocalDateTime createdAt; // Timestamp when the user_preference was created
    private LocalDateTime updatedAt; // Timestamp when the user_preference was last updated
    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus; // Status to track the entity

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @PreUpdate
    public void update(){
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void create(){
        createdAt = LocalDateTime.now();
        entityStatus = EntityStatus.ACTIVE;
    }
}
