package com.pgcompliance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TenantRequestDto {

    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 100, message = "Tenant name must contain between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String mobileNumber;

    
    @Email(message = "Enter a valid email address")
    @Size(max = 150, message = "Email address must not exceed 150 characters")
    private String email;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    @NotNull(message = "Room ID is required")
    private Long roomId;
}