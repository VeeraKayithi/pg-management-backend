package com.pgcompliance.controller;

import com.pgcompliance.dto.TenantAccountRequestDto;
import com.pgcompliance.dto.TenantAccountResponseDto;
import com.pgcompliance.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pgcompliance.dto.StaffAccountRequestDto;
import com.pgcompliance.dto.StaffAccountResponseDto;
import com.pgcompliance.dto.StaffAccountStatusRequestDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping("/tenant-account")
  public ResponseEntity<TenantAccountResponseDto> createTenantAccount(
      @Valid @RequestBody TenantAccountRequestDto request) {

    TenantAccountResponseDto response = userService.createTenantAccount(
        request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  @PostMapping("/staff-account")
  public ResponseEntity<StaffAccountResponseDto> createStaffAccount(
      @Valid @RequestBody StaffAccountRequestDto request) {

    StaffAccountResponseDto response = userService.createStaffAccount(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  @GetMapping("/staff-accounts")
  public ResponseEntity<List<StaffAccountResponseDto>> getStaffAccounts() {

    return ResponseEntity.ok(
        userService.getStaffAccounts());
  }

  @PatchMapping("/staff-accounts/{userId}/active")
  public ResponseEntity<StaffAccountResponseDto> updateStaffAccountStatus(
      @PathVariable Long userId,
      @Valid @RequestBody StaffAccountStatusRequestDto request) {

    return ResponseEntity.ok(
        userService.updateStaffAccountStatus(
            userId,
            request.getActive()));
  }
}