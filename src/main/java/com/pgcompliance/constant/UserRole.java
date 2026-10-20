package com.pgcompliance.constant;

public enum UserRole {

    ADMIN, // Super Admin - all buildings
    BUILDING_ADMIN, // Full access to one assigned building
    SUPERVISOR, // Read-only access to one assigned building
    TENANT // keep if already present
}