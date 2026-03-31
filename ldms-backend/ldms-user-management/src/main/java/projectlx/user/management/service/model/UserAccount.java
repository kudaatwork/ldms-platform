package projectlx.user.management.service.model;

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
@ToString(onlyExplicitlyIncluded = true)
public class UserAccount {
    // User Account Information
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for the user_account
    private String phoneNumber; // Unique phone number for the user
    private String accountNumber; // Unique account number for the user
    private Boolean isAccountLocked; // Indicates if the account is locked
    private LocalDateTime lastLoginAt; // Timestamp of the user's last login
    private LocalDateTime createdAt; // Timestamp when the user_account was created
    private LocalDateTime updatedAt; // Timestamp when the user_account was last updated
    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus; // Status to track the entity

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
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
