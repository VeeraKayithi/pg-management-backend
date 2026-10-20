package com.pgcompliance.service.impl;

import com.pgcompliance.constant.NotificationEventType;
import com.pgcompliance.constant.NotificationSeverity;
import com.pgcompliance.constant.RoomStatus;
import com.pgcompliance.constant.SharingType;
import com.pgcompliance.constant.TenantStatus;

import com.pgcompliance.dto.TenantProfileResponseDto;
import com.pgcompliance.dto.TenantRequestDto;
import com.pgcompliance.dto.TenantResponseDto;

import com.pgcompliance.entity.Room;
import com.pgcompliance.entity.Tenant;
import com.pgcompliance.entity.User;

import com.pgcompliance.exception.ResourceNotFoundException;

import com.pgcompliance.repository.RoomRepository;
import com.pgcompliance.repository.TenantRepository;
import com.pgcompliance.repository.UserRepository;
import com.pgcompliance.service.BuildingAccessService;
import com.pgcompliance.service.NotificationService;
import com.pgcompliance.service.TenantService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

        private final TenantRepository tenantRepository;

        private final RoomRepository roomRepository;

        private final UserRepository userRepository;

        private final NotificationService notificationService;

        private final BuildingAccessService buildingAccessService;

        @Override
        @Transactional
        public TenantResponseDto createTenant(
                        TenantRequestDto request) {

                String normalizedName = normalizeName(request.getName());

                String normalizedMobileNumber = normalizeMobileNumber(
                                request.getMobileNumber());

                String normalizedEmail = normalizeEmail(
                                request.getEmail());

                /*
                 * Check duplicate email only when
                 * an actual email value is present.
                 */
                if (normalizedEmail != null
                                && tenantRepository
                                                .existsByEmailIgnoreCase(
                                                                normalizedEmail)) {

                        throw new IllegalStateException(
                                        "A tenant already exists with this email address");
                }

                Room requestedRoom = findRoom(request.getRoomId());

                buildingAccessService.validateWriteAccess(
                                requestedRoom
                                                .getBuilding()
                                                .getBuildingId());

                validateRoomForAssignment(
                                requestedRoom,
                                null);

                Optional<Tenant> existingTenantByMobile = tenantRepository
                                .findByMobileNumber(
                                                normalizedMobileNumber);

                /*
                 * Returning Tenant workflow.
                 *
                 * If a Tenant already exists with the same mobile
                 * number but is not ACTIVE, reactivate the same
                 * Tenant record instead of creating a duplicate.
                 */
                if (existingTenantByMobile.isPresent()) {

                        Tenant existingTenant = existingTenantByMobile.get();

                        if (existingTenant.getRoom() == null) {
                                throw new IllegalStateException(
                                                "Existing tenant does not have an assigned room");
                        }

                        buildingAccessService.validateWriteAccess(
                                        existingTenant
                                                        .getRoom()
                                                        .getBuilding()
                                                        .getBuildingId());

                        if (existingTenant.getTenantStatus() == TenantStatus.ACTIVE) {

                                throw new IllegalStateException(
                                                "Tenant already exists with this mobile number");
                        }

                        validateEmailForUpdate(
                                        normalizedEmail,
                                        existingTenant.getTenantId());

                        Room previousRoom = existingTenant.getRoom();

                        existingTenant.setName(
                                        normalizedName);

                        existingTenant.setMobileNumber(
                                        normalizedMobileNumber);

                        existingTenant.setEmail(
                                        normalizedEmail);

                        /*
                         * Joining date becomes the
                         * actual date of the new stay.
                         */
                        existingTenant.setJoiningDate(
                                        request.getJoiningDate());

                        /*
                         * createdAt is intentionally
                         * NOT changed here.
                         */
                        existingTenant.setTenantStatus(
                                        TenantStatus.ACTIVE);

                        existingTenant.setRoom(
                                        requestedRoom);

                        Tenant reactivatedTenant = tenantRepository.saveAndFlush(
                                        existingTenant);

                        if (previousRoom != null
                                        && !previousRoom
                                                        .getRoomId()
                                                        .equals(
                                                                        requestedRoom
                                                                                        .getRoomId())) {

                                synchronizeRoomStatus(
                                                previousRoom);
                        }

                        synchronizeRoomStatus(
                                        requestedRoom);

                        createRoomAssignmentNotificationIfAccountExists(
                                        reactivatedTenant,
                                        requestedRoom);

                        return mapToResponse(
                                        reactivatedTenant);
                }

                /*
                 * No second email duplicate check is required here.
                 * It has already been checked above.
                 */

                Tenant tenant = Tenant.builder()
                                .name(
                                                normalizedName)
                                .mobileNumber(
                                                normalizedMobileNumber)
                                .email(
                                                normalizedEmail)
                                .joiningDate(
                                                request.getJoiningDate())
                                .createdAt(
                                                LocalDateTime.now())
                                .tenantStatus(
                                                TenantStatus.ACTIVE)
                                .room(
                                                requestedRoom)
                                .build();

                Tenant savedTenant = tenantRepository.saveAndFlush(
                                tenant);

                synchronizeRoomStatus(
                                requestedRoom);

                /*
                 * A newly created Tenant normally does not yet
                 * have a User account.
                 *
                 * The initial room assignment notification
                 * will therefore be created later when the
                 * portal account is created.
                 */
                return mapToResponse(
                                savedTenant);
        }

        @Override
        @Transactional(readOnly = true)
        public List<TenantResponseDto> getAllTenants() {

                /*
                 * Super Admin can see tenants
                 * from every building.
                 */
                if (buildingAccessService.isSuperAdmin()) {

                        return tenantRepository
                                        .findAll()
                                        .stream()
                                        .map(this::mapToResponse)
                                        .toList();
                }

                /*
                 * BUILDING_ADMIN and SUPERVISOR
                 * can see only tenants belonging
                 * to their assigned building.
                 */
                Long buildingId = buildingAccessService.getAccessibleBuildingId();

                buildingAccessService.validateReadAccess(
                                buildingId);

                return tenantRepository
                                .findByRoomBuildingBuildingId(buildingId)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public TenantResponseDto getTenantById(
                        Long tenantId) {

                Tenant tenant = findTenant(tenantId);

                if (tenant.getRoom() == null) {
                        throw new IllegalStateException(
                                        "Tenant does not have an assigned room");
                }

                buildingAccessService.validateReadAccess(
                                tenant.getRoom()
                                                .getBuilding()
                                                .getBuildingId());

                return mapToResponse(tenant);
        }

        @Override
        @Transactional
        public TenantResponseDto updateTenant(
                        Long tenantId,
                        TenantRequestDto request) {

                Tenant tenant = findTenant(
                                tenantId);

                if (tenant.getRoom() == null) {
                        throw new IllegalStateException(
                                        "Tenant does not have an assigned room");
                }

                buildingAccessService.validateWriteAccess(
                                tenant.getRoom()
                                                .getBuilding()
                                                .getBuildingId());

                String normalizedName = normalizeName(
                                request.getName());

                String normalizedMobileNumber = normalizeMobileNumber(
                                request.getMobileNumber());

                String normalizedEmail = normalizeEmail(
                                request.getEmail());

                validateMobileForUpdate(
                                normalizedMobileNumber,
                                tenantId);

                validateEmailForUpdate(
                                normalizedEmail,
                                tenantId);

                Room oldRoom = tenant.getRoom();

                Room requestedRoom = findRoom(
                                request.getRoomId());

                buildingAccessService.validateWriteAccess(
                                requestedRoom
                                                .getBuilding()
                                                .getBuildingId());

                boolean roomChanged = oldRoom == null
                                || !oldRoom
                                                .getRoomId()
                                                .equals(
                                                                requestedRoom
                                                                                .getRoomId());

                if (roomChanged
                                && tenant.getTenantStatus() == TenantStatus.ACTIVE) {

                        validateRoomForAssignment(
                                        requestedRoom,
                                        tenantId);
                }

                tenant.setName(
                                normalizedName);

                tenant.setMobileNumber(
                                normalizedMobileNumber);

                tenant.setEmail(
                                normalizedEmail);

                /*
                 * Admin may correct the actual
                 * joining date if required.
                 */
                tenant.setJoiningDate(
                                request.getJoiningDate());

                /*
                 * createdAt must never be changed.
                 */
                tenant.setRoom(
                                requestedRoom);

                Tenant updatedTenant = tenantRepository.saveAndFlush(
                                tenant);

                if (roomChanged
                                && oldRoom != null) {

                        synchronizeRoomStatus(
                                        oldRoom);
                }

                synchronizeRoomStatus(
                                requestedRoom);

                if (roomChanged) {

                        createRoomTransferNotification(
                                        updatedTenant,
                                        oldRoom,
                                        requestedRoom);
                }

                return mapToResponse(
                                updatedTenant);
        }

        @Override
        @Transactional
        public void markTenantAsLeft(
                        Long tenantId) {

                Tenant tenant = findTenant(
                                tenantId);

                if (tenant.getRoom() == null) {
                        throw new IllegalStateException(
                                        "Tenant does not have an assigned room");
                }

                buildingAccessService.validateWriteAccess(
                                tenant.getRoom()
                                                .getBuilding()
                                                .getBuildingId());

                if (tenant.getTenantStatus() == TenantStatus.LEFT) {

                        throw new IllegalStateException(
                                        "Tenant is already marked as LEFT");
                }

                Room assignedRoom = tenant.getRoom();

                tenant.setTenantStatus(
                                TenantStatus.LEFT);

                /*
                 * Flush status before occupancy recalculation.
                 */
                tenantRepository.saveAndFlush(
                                tenant);

                if (assignedRoom != null) {

                        synchronizeRoomStatus(
                                        assignedRoom);
                }

                createTenantLeftNotification(
                                tenant);
        }

        @Override
        @Transactional(readOnly = true)
        public TenantProfileResponseDto getMyProfile(
                        String username) {

                User user = userRepository
                                .findByUsername(
                                                username)
                                .orElseThrow(
                                                () -> new ResourceNotFoundException(
                                                                "User account not found"));

                Tenant tenant = user.getTenant();

                if (tenant == null) {

                        throw new ResourceNotFoundException(
                                        "No tenant profile is linked to this user account");
                }

                Room room = tenant.getRoom();

                if (room == null) {

                        throw new ResourceNotFoundException(
                                        "No room is assigned to this tenant");
                }

                return TenantProfileResponseDto
                                .builder()
                                .tenantId(
                                                tenant.getTenantId())
                                .name(
                                                tenant.getName())
                                .mobileNumber(
                                                tenant.getMobileNumber())
                                .email(
                                                tenant.getEmail())
                                .joiningDate(
                                                tenant.getJoiningDate())
                                .tenantStatus(
                                                tenant.getTenantStatus())
                                .roomId(
                                                room.getRoomId())
                                .roomNumber(
                                                room.getRoomNumber())
                                .sharingType(
                                                room.getSharingType()
                                                                .name())
                                .roomStatus(
                                                room.getRoomStatus()
                                                                .name())
                                .buildingId(
                                                room.getBuilding()
                                                                .getBuildingId())
                                .buildingName(
                                                room.getBuilding()
                                                                .getBuildingName())
                                .username(
                                                user.getUsername())
                                .roomType(
                                                room.getRoomType() == null
                                                                ? null
                                                                : room.getRoomType().name())
                                .build();
        }

        private Tenant findTenant(
                        Long tenantId) {

                return tenantRepository
                                .findById(
                                                tenantId)
                                .orElseThrow(
                                                () -> new ResourceNotFoundException(
                                                                "Tenant not found with id: "
                                                                                + tenantId));
        }

        private Room findRoom(
                        Long roomId) {

                return roomRepository
                                .findById(
                                                roomId)
                                .orElseThrow(
                                                () -> new ResourceNotFoundException(
                                                                "Room not found with id: "
                                                                                + roomId));
        }

        private void validateMobileForUpdate(
                        String normalizedMobileNumber,
                        Long currentTenantId) {

                tenantRepository
                                .findByMobileNumber(
                                                normalizedMobileNumber)
                                .filter(
                                                existingTenant -> !existingTenant
                                                                .getTenantId()
                                                                .equals(
                                                                                currentTenantId))
                                .ifPresent(
                                                existingTenant -> {

                                                        throw new IllegalStateException(
                                                                        "Another tenant already exists with this mobile number");
                                                });
        }

        private void validateEmailForUpdate(
                        String normalizedEmail,
                        Long currentTenantId) {

                /*
                 * Missing email is allowed.
                 */
                if (normalizedEmail == null) {
                        return;
                }

                tenantRepository
                                .findByEmailIgnoreCase(
                                                normalizedEmail)
                                .filter(
                                                existingTenant -> !existingTenant
                                                                .getTenantId()
                                                                .equals(
                                                                                currentTenantId))
                                .ifPresent(
                                                existingTenant -> {

                                                        throw new IllegalStateException(
                                                                        "Another tenant already exists with this email address");
                                                });
        }

        private void validateRoomForAssignment(
                        Room room,
                        Long currentTenantId) {

                if (room.getRoomStatus() == RoomStatus.BLOCKED
                                || room.getRoomStatus() == RoomStatus.UNDER_REPAIR) {

                        throw new IllegalStateException(
                                        "Room is not available for tenant assignment");
                }

                long currentOccupancy = getActiveOccupancy(
                                room.getRoomId());

                /*
                 * Protect existing Tenant when validating
                 * the Tenant's current room.
                 */
                if (currentTenantId != null) {

                        boolean currentTenantAlreadyInRoom = tenantRepository
                                        .findById(
                                                        currentTenantId)
                                        .map(
                                                        existingTenant -> existingTenant
                                                                        .getRoom() != null
                                                                        && existingTenant
                                                                                        .getRoom()
                                                                                        .getRoomId()
                                                                                        .equals(
                                                                                                        room.getRoomId())
                                                                        && existingTenant
                                                                                        .getTenantStatus() == TenantStatus.ACTIVE)
                                        .orElse(
                                                        false);

                        if (currentTenantAlreadyInRoom) {

                                currentOccupancy--;
                        }
                }

                int capacity = getCapacity(
                                room.getSharingType());

                if (currentOccupancy >= capacity) {

                        throw new IllegalStateException(
                                        "Room capacity exceeded");
                }
        }

        private long getActiveOccupancy(
                        Long roomId) {

                return tenantRepository
                                .countByRoomRoomIdAndTenantStatus(
                                                roomId,
                                                TenantStatus.ACTIVE);
        }

        private void synchronizeRoomStatus(
                        Room room) {

                long activeOccupancy = getActiveOccupancy(
                                room.getRoomId());

                int capacity = getCapacity(
                                room.getSharingType());

                RoomStatus calculatedStatus = activeOccupancy >= capacity
                                ? RoomStatus.OCCUPIED
                                : RoomStatus.AVAILABLE;

                /*
                 * Do not override Admin maintenance states.
                 */
                if (room.getRoomStatus() != RoomStatus.BLOCKED
                                && room.getRoomStatus() != RoomStatus.UNDER_REPAIR) {

                        room.setRoomStatus(
                                        calculatedStatus);

                        roomRepository.save(
                                        room);
                }
        }

        private void createRoomAssignmentNotificationIfAccountExists(
                        Tenant tenant,
                        Room room) {

                userRepository
                                .findByTenantTenantId(
                                                tenant.getTenantId())
                                .ifPresent(
                                                tenantUser ->

                                                notificationService
                                                                .createNotification(
                                                                                tenantUser
                                                                                                .getUserId(),

                                                                                NotificationEventType.TENANT_ROOM_ASSIGNED,

                                                                                NotificationSeverity.INFO,

                                                                                "TENANT",

                                                                                "Room assigned",

                                                                                "You have been assigned to Room "
                                                                                                + room
                                                                                                                .getRoomNumber()
                                                                                                + " in "
                                                                                                + room
                                                                                                                .getBuilding()
                                                                                                                .getBuildingName()
                                                                                                + ".",

                                                                                "/tenant/dashboard",

                                                                                "TENANT",

                                                                                tenant
                                                                                                .getTenantId(),

                                                                                "TENANT_ROOM_ASSIGNED:"
                                                                                                + tenant
                                                                                                                .getTenantId()
                                                                                                + ":"
                                                                                                + room
                                                                                                                .getRoomId()
                                                                                                + ":"
                                                                                                + tenant
                                                                                                                .getJoiningDate()));
        }

        private void createRoomTransferNotification(
                        Tenant tenant,
                        Room oldRoom,
                        Room newRoom) {

                userRepository
                                .findByTenantTenantId(
                                                tenant.getTenantId())
                                .ifPresent(
                                                tenantUser -> {

                                                        String oldRoomDescription = oldRoom == null
                                                                        ? "an unassigned room"
                                                                        : roomDescription(
                                                                                        oldRoom);

                                                        notificationService
                                                                        .createNotification(
                                                                                        tenantUser
                                                                                                        .getUserId(),

                                                                                        NotificationEventType.TENANT_ROOM_TRANSFERRED,

                                                                                        NotificationSeverity.INFO,

                                                                                        "TENANT",

                                                                                        "Room assignment updated",

                                                                                        "Your accommodation has been transferred from "
                                                                                                        + oldRoomDescription
                                                                                                        + " to "
                                                                                                        + roomDescription(
                                                                                                                        newRoom)
                                                                                                        + ".",

                                                                                        "/tenant/dashboard",

                                                                                        "TENANT",

                                                                                        tenant
                                                                                                        .getTenantId(),

                                                                                        "TENANT_ROOM_TRANSFERRED:"
                                                                                                        + tenant
                                                                                                                        .getTenantId()
                                                                                                        + ":"
                                                                                                        + (oldRoom == null
                                                                                                                        ? "NONE"
                                                                                                                        : oldRoom
                                                                                                                                        .getRoomId())
                                                                                                        + ":"
                                                                                                        + newRoom
                                                                                                                        .getRoomId()
                                                                                                        + ":"
                                                                                                        + System
                                                                                                                        .currentTimeMillis());
                                                });
        }

        private void createTenantLeftNotification(
                        Tenant tenant) {

                userRepository
                                .findByTenantTenantId(
                                                tenant.getTenantId())
                                .ifPresent(
                                                tenantUser ->

                                                notificationService
                                                                .createNotification(
                                                                                tenantUser
                                                                                                .getUserId(),

                                                                                NotificationEventType.TENANT_MARKED_LEFT,

                                                                                NotificationSeverity.INFO,

                                                                                "TENANT",

                                                                                "Resident status updated",

                                                                                "Your Nandu PG resident status has been updated to LEFT.",

                                                                                "/tenant/dashboard",

                                                                                "TENANT",

                                                                                tenant
                                                                                                .getTenantId(),

                                                                                "TENANT_MARKED_LEFT:"
                                                                                                + tenant
                                                                                                                .getTenantId()
                                                                                                + ":"
                                                                                                + tenant
                                                                                                                .getJoiningDate()));
        }

        private String roomDescription(
                        Room room) {

                return "Room "
                                + room.getRoomNumber()
                                + " in "
                                + room.getBuilding()
                                                .getBuildingName();
        }

        private String normalizeName(
                        String name) {

                return name
                                .trim()
                                .replaceAll(
                                                "\\s+",
                                                " ");
        }

        private String normalizeMobileNumber(
                        String mobileNumber) {

                return mobileNumber
                                .replaceAll(
                                                "\\s+",
                                                "")
                                .trim();
        }

        private String normalizeEmail(
                        String email) {

                if (email == null
                                || email.isBlank()) {

                        return null;
                }

                return email
                                .trim()
                                .toLowerCase();
        }

        private TenantResponseDto mapToResponse(
                        Tenant tenant) {

                Room room = tenant.getRoom();

                Optional<User> linkedUser = userRepository
                                .findByTenantTenantId(
                                                tenant.getTenantId());

                boolean portalAccountCreated = linkedUser.isPresent();

                boolean portalAccountActive = linkedUser
                                .map(
                                                user -> Boolean.TRUE.equals(
                                                                user.getActive()))
                                .orElse(
                                                false);

                boolean emailVerified = linkedUser
                                .map(
                                                user -> Boolean.TRUE.equals(
                                                                user.getEmailVerified()))
                                .orElse(
                                                false);

                String portalUsername = linkedUser
                                .map(
                                                User::getUsername)
                                .orElse(
                                                "");

                return TenantResponseDto
                                .builder()
                                .tenantId(
                                                tenant.getTenantId())
                                .name(
                                                tenant.getName())
                                .mobileNumber(
                                                tenant.getMobileNumber())
                                .email(
                                                tenant.getEmail())
                                .joiningDate(
                                                tenant.getJoiningDate())
                                .createdAt(
                                                tenant.getCreatedAt())
                                .roomId(
                                                room == null
                                                                ? null
                                                                : room.getRoomId())
                                .roomNumber(
                                                room == null
                                                                ? null
                                                                : room.getRoomNumber())
                                .tenantStatus(
                                                tenant.getTenantStatus())
                                .portalAccountCreated(
                                                portalAccountCreated)
                                .portalAccountActive(
                                                portalAccountActive)
                                .emailVerified(
                                                emailVerified)
                                .portalUsername(
                                                portalUsername)
                                .roomType(
                                                room == null || room.getRoomType() == null
                                                                ? null
                                                                : room.getRoomType().name())
                                .build();
        }

        private int getCapacity(
                        SharingType sharingType) {

                return switch (sharingType) {

                        case SINGLE -> 1;

                        case DOUBLE -> 2;

                        case TRIPLE -> 3;

                        case FOUR -> 4;
                };
        }
}