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
@Table
@Getter
@Setter
@ToString
public class UserSecurity {
    // User Security Information
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for the user_account
    private String securityQuestion_1; // First security question
    private String securityAnswer_1; // Answer to the first security question
    private String securityQuestion_2; // Second security question
    private String securityAnswer_2; // Answer to the second security question
    private String twoFactorAuthSecret; // Secret for two-factor authentication
    private Boolean isTwoFactorEnabled; // Indicates if 2FA is enabled
    private LocalDateTime createdAt; // Timestamp when the user_security was created
    private LocalDateTime updatedAt; // Timestamp when the user_security was last updated
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
