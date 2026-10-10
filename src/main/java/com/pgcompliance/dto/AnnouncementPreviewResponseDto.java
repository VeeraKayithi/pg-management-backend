package com.pgcompliance.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnnouncementPreviewResponseDto {

    private long activeTenantCount;

    private long portalAccountTenantCount;

    private long eligibleTenantCount;

    private long inAppEligibleCount;

    private long emailEligibleCount;

    private long inAppSkippedCount;

    private long emailSkippedCount;

    private long tenantsWithoutPortalAccount;
}