package com.pgcompliance.dto;

import com.pgcompliance.constant.TenantStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TenantResponseDto {

    private Long tenantId;

    private String name;

    private String mobileNumber;

    private String email;

    private LocalDate joiningDate;

    private LocalDateTime createdAt;

    private String roomType;

    private String roomNumber;

    private Long roomId;

    private TenantStatus tenantStatus;

    private Boolean portalAccountCreated;

    private Boolean portalAccountActive;

    private Boolean emailVerified;

    private String portalUsername;
}