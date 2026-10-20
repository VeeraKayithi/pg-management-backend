package com.pgcompliance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffAccountStatusRequestDto {

    @NotNull(message = "Active status is required")
    private Boolean active;
}