package com.pgcompliance.service;

import com.pgcompliance.dto.StaffAccountRequestDto;
import com.pgcompliance.dto.StaffAccountResponseDto;
import com.pgcompliance.dto.TenantAccountRequestDto;
import com.pgcompliance.dto.TenantAccountResponseDto;

import java.util.List;

public interface UserService {

    TenantAccountResponseDto createTenantAccount(
            TenantAccountRequestDto request);

    StaffAccountResponseDto createStaffAccount(
            StaffAccountRequestDto request);

    List<StaffAccountResponseDto> getStaffAccounts();

    StaffAccountResponseDto updateStaffAccountStatus(
            Long userId,
            Boolean active);
}