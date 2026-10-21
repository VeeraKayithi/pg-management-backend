package com.pgcompliance.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TenantStayHistoryDto(
        Long stayId,
        LocalDate joiningDate,
        LocalDate leavingDate,
        String status,
        Long roomId,
        String roomNumber,
        String buildingName,
        boolean departureDateUnknown,
        List<Transfer> transfers
) {
    public record Transfer(
            Long transferId,
            Long fromRoomId,
            String fromRoom,
            Long toRoomId,
            String toRoom,
            LocalDateTime transferredAt
    ) {}
}
