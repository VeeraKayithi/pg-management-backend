package com.pgcompliance.service;

import com.pgcompliance.entity.User;

public interface BuildingAccessService {

    User getCurrentUser();

    boolean isSuperAdmin();

    Long getAccessibleBuildingId();

    void validateReadAccess(Long buildingId);

    void validateWriteAccess(Long buildingId);

    void validateSuperAdminAccess();
}