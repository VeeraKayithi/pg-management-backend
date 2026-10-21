package com.pgcompliance.repository;

import com.pgcompliance.entity.TenantStay;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TenantStayRepository extends JpaRepository<TenantStay, Long> {

    List<TenantStay> findByTenantTenantIdOrderByJoiningDateDescStayIdDesc(Long tenantId);

    Optional<TenantStay> findByTenantTenantIdAndStayStatus(
            Long tenantId, TenantStay.StayStatus status);
}
