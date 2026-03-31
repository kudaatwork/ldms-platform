package projectlx.user.management.service.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_type", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "user_type_name", "description"})})
@Getter
@Setter
@ToString
public class UserType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id; // Unique identifier for the user_type
    private String userTypeName; // Name of the user type (e.g., ACCOUNTANT, FARMER, DRIVER)
    private String description; // Description of the user type
    private LocalDateTime createdAt; // Timestamp when the user_type was created
    private LocalDateTime updatedAt; // Timestamp when the user_type was last updated

    @OneToMany(mappedBy = "userType", cascade = CascadeType.ALL)
    private List<User> users;

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus; // Status to track the entity

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
