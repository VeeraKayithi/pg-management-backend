package com.pgcompliance.repository;

import com.pgcompliance.constant.UserRole;
import com.pgcompliance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByTenantTenantId(Long tenantId);

    Optional<User> findByTenantTenantId(Long tenantId);

    List<User> findByRoleAndActiveTrue(UserRole role);

    List<User> findByRoleIn(List<UserRole> roles);
}
