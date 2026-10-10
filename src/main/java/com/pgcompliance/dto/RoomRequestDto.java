package com.pgcompliance.dto;

import com.pgcompliance.constant.RoomStatus;
import com.pgcompliance.constant.RoomType;
import com.pgcompliance.constant.SharingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomRequestDto {

    @NotNull
    private Long buildingId;

    @NotBlank
    private String roomNumber;

    @NotNull
    private SharingType sharingType;

    @NotNull(message = "Room type is required")
    private RoomType roomType;

    @NotNull
    private RoomStatus roomStatus;
}