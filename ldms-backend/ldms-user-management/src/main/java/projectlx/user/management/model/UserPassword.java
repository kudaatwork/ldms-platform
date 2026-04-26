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
@ToString(onlyExplicitlyIncluded = true)
public class UserPassword {
    // User Password Manager
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for a password
    private String password; // Encrypted password
    private LocalDateTime expiryDate; // Password Expiry Date
    private Boolean isPasswordExpired; // Indicates if password has expired
    private LocalDateTime createdAt; // Timestamp when the user_password was created
    private LocalDateTime updatedAt; // Timestamp when the user_password was last updated
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
