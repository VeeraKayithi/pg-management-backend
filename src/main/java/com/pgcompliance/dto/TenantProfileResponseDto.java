package com.pgcompliance.dto;

import com.pgcompliance.constant.TenantStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TenantProfileResponseDto {

  private Long tenantId;

  private String name;

  private String mobileNumber;

  private String email;

  private String roomType;
  
  private LocalDate joiningDate;

  private TenantStatus tenantStatus;

  private Long roomId;

  private String roomNumber;

  private String sharingType;

  private String roomStatus;

  private Long buildingId;

  private String buildingName;

  private String username;
}