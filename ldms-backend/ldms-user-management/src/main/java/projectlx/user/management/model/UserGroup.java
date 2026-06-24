package projectlx.user.management.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table
@Getter
@Setter
@ToString(exclude = "userRoles")
public class UserGroup {
    // Basic User group information
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for a user group
    private String name; // Name of the user group
    private String description; // Description of the user group
    /** When set, the group belongs to one organisation workspace; {@code null} = platform-wide (admin portal). */
    @jakarta.persistence.Column(name = "organization_id")
    private Long organizationId;
    /** Organisation class for workspace groups (SUPPLIER, TRANSPORT_COMPANY, etc.). */
    @jakarta.persistence.Column(name = "organization_classification", length = 50)
    private String organizationClassification;
    /** Whether this is a system-provisioned group (e.g. Administrator) that cannot be deleted. */
    @Column(name = "is_system_group", nullable = false)
    private boolean systemGroup = false;
    /** User-editable display alias for a system group (e.g. renaming "Administrator" to "Org Admins"). */
    @Column(name = "system_group_alias", length = 100)
    private String systemGroupAlias;
    private LocalDateTime createdAt; // Timestamp when the user group was created
    private LocalDateTime updatedAt; // Timestamp when the user group was last updated
    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus;

    @OneToMany(mappedBy = "userGroup")
    private List<User> users;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY,
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE
            })
    @JoinTable(
            name = "user_group_user_role",  // Name of the join table
            joinColumns = @JoinColumn(name = "user_group_id"),  // Foreign key for UserGroup
            inverseJoinColumns = @JoinColumn(name = "user_role_id")  // Foreign key for UserRole
    )
    private Set<UserRole> userRoles = new HashSet<>();

    /** Role IDs that are "locked" (default) for this system group and cannot be removed. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_group_default_roles",
            joinColumns = @JoinColumn(name = "user_group_id")
    )
    @Column(name = "user_role_id")
    private Set<Long> defaultRoleIds = new HashSet<>();

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
