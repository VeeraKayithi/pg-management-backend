package com.pgcompliance.service.impl;

import com.pgcompliance.constant.UserRole;
import com.pgcompliance.entity.User;
import com.pgcompliance.repository.UserRepository;
import com.pgcompliance.service.BuildingAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuildingAccessServiceImpl
    implements BuildingAccessService {

  private final UserRepository userRepository;

  @Override
  public User getCurrentUser() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null ||
        !authentication.isAuthenticated()) {

      throw new AccessDeniedException(
          "User is not authenticated");
    }

    String username = authentication.getName();

    return userRepository.findByUsername(username)
        .orElseThrow(() -> new AccessDeniedException(
            "Logged-in user not found"));
  }

  @Override
  public boolean isSuperAdmin() {
    return getCurrentUser().getRole() == UserRole.ADMIN;
  }

  @Override
  public Long getAccessibleBuildingId() {

    User user = getCurrentUser();

    if (user.getRole() == UserRole.ADMIN) {
      return null;
    }

    if (user.getBuilding() == null) {
      throw new AccessDeniedException(
          "No building is assigned to this account");
    }

    return user.getBuilding().getBuildingId();
  }

  @Override
  public void validateReadAccess(Long buildingId) {

    User user = getCurrentUser();

    if (user.getRole() == UserRole.ADMIN) {
      return;
    }

    if (user.getRole() != UserRole.BUILDING_ADMIN &&
        user.getRole() != UserRole.SUPERVISOR) {

      throw new AccessDeniedException(
          "You do not have permission to access building data");
    }

    validateAssignedBuilding(user, buildingId);
  }

  @Override
  public void validateSuperAdminAccess() {

    User user = getCurrentUser();

    if (user.getRole() != UserRole.ADMIN) {
      throw new AccessDeniedException(
          "Only Super Admin can perform this operation");
    }
  }

  @Override
  public void validateWriteAccess(Long buildingId) {

    User user = getCurrentUser();

    if (user.getRole() == UserRole.ADMIN) {
      return;
    }

    if (user.getRole() != UserRole.BUILDING_ADMIN) {

      throw new AccessDeniedException(
          "You do not have permission to modify building data");
    }

    validateAssignedBuilding(user, buildingId);
  }

  private void validateAssignedBuilding(
      User user,
      Long buildingId) {

    if (user.getBuilding() == null) {
      throw new AccessDeniedException(
          "No building is assigned to this account");
    }

    Long assignedBuildingId = user.getBuilding().getBuildingId();

    if (!assignedBuildingId.equals(buildingId)) {

      throw new AccessDeniedException(
          "You do not have access to this building");
    }
  }
}