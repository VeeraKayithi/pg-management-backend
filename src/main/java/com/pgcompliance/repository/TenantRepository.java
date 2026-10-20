package com.pgcompliance.repository;

import com.pgcompliance.constant.TenantStatus;
import com.pgcompliance.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    boolean existsByMobileNumber(String mobileNumber);

    long countByRoomRoomId(Long roomId);

    long countByRoomRoomIdAndTenantStatus(Long roomId, TenantStatus tenantStatus);

    Optional<Tenant> findByMobileNumber(String mobileNumber);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Tenant> findByEmailIgnoreCase(String email);

    List<Tenant> findByTenantStatusOrderByNameAsc(TenantStatus tenantStatus);

    boolean existsByEmail(String email);

    boolean existsByEmailAndTenantIdNot(String email, Long tenantId);

    List<Tenant> findByRoomBuildingBuildingId(Long buildingId);
}
