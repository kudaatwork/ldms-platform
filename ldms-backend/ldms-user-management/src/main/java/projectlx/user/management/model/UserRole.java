package projectlx.user.management.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@Getter
@Setter
@ToString(exclude = "userGroups")
public class UserRole {
    // User Roles
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for the user_role
    private String role; // Role assigned to the user (e.g., ADMIN, USER)
    private String description; // Description of the user role
    private LocalDateTime createdAt; // Timestamp when the user_role was created
    private LocalDateTime updatedAt; // Timestamp when the user_role was last updated
    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus; // Status to track the entity

    @ManyToMany(mappedBy = "userRoles")  // This field is already mapped in Student
    @JsonIgnore
    private Set<UserGroup> userGroups = new HashSet<>();

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
