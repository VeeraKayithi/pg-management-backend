package com.pgcompliance.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffAccountResponseDto {

  private Long userId;

  private String username;

  private String role;

  private Boolean active;

  private Long buildingId;

  private String buildingName;
}