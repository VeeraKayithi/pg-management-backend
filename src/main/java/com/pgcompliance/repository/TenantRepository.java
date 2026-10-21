package com.pgcompliance.repository;

import com.pgcompliance.constant.TenantStatus;
import com.pgcompliance.entity.Tenant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tenant t where t.tenantId = :tenantId")
    Optional<Tenant> findByIdForUpdate(@Param("tenantId") Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tenant t where t.mobileNumber = :mobileNumber")
    Optional<Tenant> findByMobileNumberForUpdate(@Param("mobileNumber") String mobileNumber);

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
