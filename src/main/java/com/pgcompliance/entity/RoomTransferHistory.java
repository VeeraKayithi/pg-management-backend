package com.pgcompliance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_transfer_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomTransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long transferId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stay_id", nullable = false)
    private TenantStay stay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_room_id", nullable = false)
    private Room fromRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_room_id", nullable = false)
    private Room toRoom;

    @Column(name = "transferred_at", nullable = false)
    private LocalDateTime transferredAt;

    @Column(name = "notes", length = 500)
    private String notes;
}
