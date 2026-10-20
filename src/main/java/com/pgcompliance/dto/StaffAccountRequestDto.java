package com.pgcompliance.dto;

import com.pgcompliance.constant.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffAccountRequestDto {

    @NotBlank(message = "Username is required")
    @Size(
        min = 4,
        max = 100,
        message = "Username must contain between 4 and 100 characters"
    )
    private String username;

    @NotBlank(message = "Password is required")
    @Size(
        min = 8,
        max = 100,
        message = "Password must contain at least 8 characters"
    )
    private String password;

    @NotNull(message = "Role is required")
    private UserRole role;

    @NotNull(message = "Building is required")
    private Long buildingId;
}