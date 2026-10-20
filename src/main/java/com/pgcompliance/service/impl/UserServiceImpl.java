package com.pgcompliance.service.impl;

import com.pgcompliance.constant.TenantStatus;
import com.pgcompliance.constant.UserRole;
import com.pgcompliance.dto.StaffAccountRequestDto;
import com.pgcompliance.dto.StaffAccountResponseDto;
import com.pgcompliance.dto.TenantAccountRequestDto;
import com.pgcompliance.dto.TenantAccountResponseDto;
import com.pgcompliance.entity.Building;
import com.pgcompliance.entity.Tenant;
import com.pgcompliance.entity.User;
import com.pgcompliance.exception.ResourceNotFoundException;
import com.pgcompliance.repository.BuildingRepository;
import com.pgcompliance.repository.TenantRepository;
import com.pgcompliance.repository.UserRepository;
import com.pgcompliance.service.AccountActivationService;
import com.pgcompliance.service.BuildingAccessService;
import com.pgcompliance.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;

        private final TenantRepository tenantRepository;

        private final PasswordEncoder passwordEncoder;

        private final BuildingRepository buildingRepository;

        private final BuildingAccessService buildingAccessService;

        private final AccountActivationService accountActivationService;

        @Override
        @Transactional
        public TenantAccountResponseDto createTenantAccount(
                        TenantAccountRequestDto request) {

                Tenant tenant = tenantRepository
                                .findById(request.getTenantId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Tenant not found with id: "
                                                                + request.getTenantId()));

                validateTenantForAccountCreation(tenant);

                String normalizedUsername = normalizeUsername(request.getUsername());

                validateUsernameAvailability(
                                normalizedUsername);

                User user = User.builder()
                                .username(normalizedUsername)

                                /*
                                 * The Admin no longer enters a temporary
                                 * password.
                                 *
                                 * A random value is encoded and stored as an
                                 * unusable placeholder until the Tenant opens
                                 * the activation link and creates a private
                                 * password.
                                 */
                                .password(generateUnusablePassword())

                                .role(UserRole.TENANT)

                                /*
                                 * The Tenant cannot log in until email
                                 * activation is completed.
                                 */
                                .active(false)

                                .emailVerified(false)

                                .passwordChangeRequired(true)

                                .tenant(tenant)

                                .build();

                User savedUser = userRepository.saveAndFlush(user);

                /*
                 * Generates a secure activation token, stores its
                 * hash, and sends the activation link to the email
                 * address stored on the linked Tenant.
                 */
                accountActivationService.createAndSendActivation(
                                savedUser.getUsername());

                return TenantAccountResponseDto.builder()
                                .userId(savedUser.getUserId())
                                .tenantId(tenant.getTenantId())
                                .tenantName(tenant.getName())
                                .username(savedUser.getUsername())
                                .role(savedUser.getRole().name())
                                .active(savedUser.getActive())
                                .message(
                                                "Tenant portal account created. "
                                                                + "An activation email was sent to "
                                                                + maskEmail(tenant.getEmail()))
                                .build();
        }

        private void validateTenantForAccountCreation(
                        Tenant tenant) {

                if (tenant.getTenantStatus() != TenantStatus.ACTIVE) {

                        throw new IllegalStateException(
                                        "Login account can be created only for an ACTIVE tenant");
                }

                if (userRepository.existsByTenantTenantId(
                                tenant.getTenantId())) {

                        throw new IllegalStateException(
                                        "A login account already exists for this tenant");
                }

                if (tenant.getEmail() == null
                                || tenant.getEmail().isBlank()) {

                        throw new IllegalStateException(
                                        "A valid Tenant email address is required "
                                                        + "before creating a portal account");
                }
        }

        private void validateUsernameAvailability(
                        String username) {

                if (username.length() < 4
                                || username.length() > 100) {

                        throw new IllegalStateException(
                                        "Username must contain between 4 and 100 characters");
                }

                if (userRepository.existsByUsername(
                                username)) {

                        throw new IllegalStateException(
                                        "Username already exists");
                }
        }

        private String normalizeUsername(
                        String username) {

                if (username == null
                                || username.isBlank()) {

                        throw new IllegalStateException(
                                        "Username is required");
                }

                return username
                                .trim()
                                .toLowerCase();
        }

        private String generateUnusablePassword() {

                /*
                 * UUID creates an unpredictable placeholder.
                 *
                 * BCrypt encodes it before storage. The raw value
                 * is never returned, displayed, emailed, or sent
                 * to the Tenant.
                 */
                String randomPlaceholder = UUID.randomUUID().toString();

                return passwordEncoder.encode(
                                randomPlaceholder);
        }

        private String maskEmail(
                        String email) {

                if (email == null
                                || email.isBlank()
                                || !email.contains("@")) {

                        return "the registered email address";
                }

                String[] parts = email.split("@", 2);

                String localPart = parts[0];

                String domain = parts[1];

                if (localPart.length() <= 2) {

                        return localPart.charAt(0)
                                        + "***@"
                                        + domain;
                }

                return localPart.substring(0, 2)
                                + "***@"
                                + domain;
        }

        private void validateStaffRole(UserRole role) {

                if (role != UserRole.BUILDING_ADMIN
                                && role != UserRole.SUPERVISOR) {

                        throw new IllegalStateException(
                                        "Staff role must be BUILDING_ADMIN or SUPERVISOR");
                }
        }

        private void validateStaffPassword(
                        String password) {

                if (password == null
                                || password.isBlank()) {

                        throw new IllegalStateException(
                                        "Password is required");
                }

                if (password.length() < 8) {

                        throw new IllegalStateException(
                                        "Password must contain at least 8 characters");
                }

                if (password
                                .getBytes(StandardCharsets.UTF_8).length > 72) {

                        throw new IllegalStateException(
                                        "Password is too long");
                }
        }

        @Override
        @Transactional
        public StaffAccountResponseDto createStaffAccount(
                        StaffAccountRequestDto request) {

                buildingAccessService.validateSuperAdminAccess();

                validateStaffRole(request.getRole());

                String normalizedUsername = normalizeUsername(request.getUsername());

                validateUsernameAvailability(
                                normalizedUsername);

                validateStaffPassword(
                                request.getPassword());

                Building building = buildingRepository
                                .findById(request.getBuildingId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Building not found with id: "
                                                                + request.getBuildingId()));

                User user = User.builder()
                                .username(normalizedUsername)
                                .password(
                                                passwordEncoder.encode(
                                                                request.getPassword()))
                                .role(request.getRole())
                                .active(true)

                                /*
                                 * Staff accounts do not use the
                                 * Tenant email-activation workflow.
                                 */
                                .emailVerified(true)
                                .passwordChangeRequired(false)

                                .building(building)
                                .tenant(null)
                                .build();

                User savedUser = userRepository.saveAndFlush(user);

                return mapToStaffAccountResponse(savedUser);
        }

        @Override
        @Transactional(readOnly = true)
        public List<StaffAccountResponseDto> getStaffAccounts() {

                buildingAccessService.validateSuperAdminAccess();

                return userRepository
                                .findByRoleIn(
                                                List.of(
                                                                UserRole.BUILDING_ADMIN,
                                                                UserRole.SUPERVISOR))
                                .stream()
                                .sorted(
                                                Comparator
                                                                .comparing(
                                                                                (User user) -> user.getBuilding()
                                                                                                .getBuildingName(),
                                                                                String.CASE_INSENSITIVE_ORDER)
                                                                .thenComparing(
                                                                                User::getUsername,
                                                                                String.CASE_INSENSITIVE_ORDER))
                                .map(this::mapToStaffAccountResponse)
                                .toList();
        }

        @Override
        @Transactional
        public StaffAccountResponseDto updateStaffAccountStatus(
                        Long userId,
                        Boolean active) {

                buildingAccessService.validateSuperAdminAccess();

                User user = userRepository
                                .findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "User not found with id: "
                                                                + userId));

                validateStaffRole(user.getRole());

                user.setActive(active);

                User savedUser = userRepository.saveAndFlush(user);

                return mapToStaffAccountResponse(savedUser);
        }

        private StaffAccountResponseDto mapToStaffAccountResponse(
                        User user) {

                Building building = user.getBuilding();

                return StaffAccountResponseDto.builder()
                                .userId(user.getUserId())
                                .username(user.getUsername())
                                .role(user.getRole().name())
                                .active(user.getActive())
                                .buildingId(
                                                building != null
                                                                ? building.getBuildingId()
                                                                : null)
                                .buildingName(
                                                building != null
                                                                ? building.getBuildingName()
                                                                : null)
                                .build();
        }
}