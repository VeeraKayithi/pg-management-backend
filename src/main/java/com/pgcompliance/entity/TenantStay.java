package com.pgcompliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_stays")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantStay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stay_id")
    private Long stayId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // Current room for this stay. Previous rooms are recorded in room_transfer_history.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "leaving_date")
    private LocalDate leavingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "stay_status", nullable = false, length = 20)
    private StayStatus stayStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum StayStatus {
        ACTIVE, COMPLETED
    }
}
