package com.pgcompliance.entity;

import com.pgcompliance.constant.TenantStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tenant_mobile", columnNames = "mobile_number"),
    @UniqueConstraint(name = "uk_tenant_email", columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long tenantId;

  @Column(nullable = false)
  private String name;

  @Column(name = "mobile_number", nullable = false, unique = true)
  private String mobileNumber;

  @Column(name = "email", unique = true, length = 150)
  private String email;

  @Column(name = "joining_date", nullable = false)
  private LocalDate joiningDate;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TenantStatus tenantStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id")
  private Room room;
}