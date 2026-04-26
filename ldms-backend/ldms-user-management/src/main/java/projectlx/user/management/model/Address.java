package projectlx.user.management.model;

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
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import projectlx.user.management.utils.dtos.AddressDto;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_address", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"location_address_id"})
})
@Getter
@Setter
@ToString
public class Address {
    // User Address Information
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique identifier for the user_address

    @Column(name = "location_address_id", unique = true)
    private Long locationAddressId; // Reference to address in Location Service
    
    private LocalDateTime createdAt; // Timestamp when the user was created
    private LocalDateTime updatedAt; // Timestamp when the user was last updated

    @Enumerated(EnumType.STRING)
    private EntityStatus entityStatus; // Status to track the entity

    @OneToMany(mappedBy = "address", cascade = CascadeType.ALL)
    private List<User> users;
    
    @Transient
    private AddressDto addressDetails; // Transient field to hold address details from Location Service

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
