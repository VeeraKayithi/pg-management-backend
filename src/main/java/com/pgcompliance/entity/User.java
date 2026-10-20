package com.pgcompliance.entity;

import com.pgcompliance.constant.UserRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean emailVerified;

    @Column(nullable = false)
    private Boolean passwordChangeRequired;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @OneToOne
    @JoinColumn(name = "tenant_id", unique = true)
    private Tenant tenant;

    @PrePersist
    void applyDefaults() {
        if (active == null) {
            active = false;
        }
        if (emailVerified == null) {
            emailVerified = false;
        }
        if (passwordChangeRequired == null) {
            passwordChangeRequired = true;
        }
    }
}
