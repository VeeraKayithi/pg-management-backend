package com.pgcompliance.entity;

import com.pgcompliance.constant.RoomStatus;
import com.pgcompliance.constant.RoomType;
import com.pgcompliance.constant.SharingType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {
                "building_id",
                "room_number"
        })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    @Column(nullable = false)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    private SharingType sharingType;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type")
    private RoomType roomType;

    @Enumerated(EnumType.STRING)
    private RoomStatus roomStatus;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;
}