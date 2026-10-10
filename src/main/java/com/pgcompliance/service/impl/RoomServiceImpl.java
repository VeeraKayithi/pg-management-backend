package com.pgcompliance.service.impl;

import com.pgcompliance.constant.SharingType;
import com.pgcompliance.dto.RoomRequestDto;
import com.pgcompliance.dto.RoomResponseDto;
import com.pgcompliance.entity.Building;
import com.pgcompliance.entity.Room;
import com.pgcompliance.exception.ResourceNotFoundException;
import com.pgcompliance.repository.BuildingRepository;
import com.pgcompliance.repository.RoomRepository;
import com.pgcompliance.repository.TenantRepository;
import com.pgcompliance.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

        private final RoomRepository roomRepository;
        private final BuildingRepository buildingRepository;
        private final TenantRepository tenantRepository;

        @Override
        @Transactional
        public RoomResponseDto createRoom(RoomRequestDto request) {
                Building building = buildingRepository
                                .findById(request.getBuildingId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Building not found with id: "
                                                                + request.getBuildingId()));

                if (roomRepository.existsByBuildingBuildingIdAndRoomNumber(
                                request.getBuildingId(),
                                request.getRoomNumber())) {
                        throw new IllegalStateException(
                                        "Room already exists in building");
                }

                Room room = Room.builder()
                                .roomNumber(request.getRoomNumber())
                                .sharingType(request.getSharingType())
                                .roomType(request.getRoomType())
                                .roomStatus(request.getRoomStatus())
                                .building(building)
                                .createdAt(LocalDateTime.now())
                                .build();

                return mapToResponse(roomRepository.save(room));
        }

        @Override
        @Transactional(readOnly = true)
        public List<RoomResponseDto> getAllRooms() {
                return roomRepository
                                .findAllByOrderByBuildingBuildingIdAscRoomNumberAsc()
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public RoomResponseDto getRoomById(Long roomId) {
                Room room = roomRepository.findById(roomId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Room not found with id: " + roomId));

                return mapToResponse(room);
        }

        @Override
        @Transactional
        public RoomResponseDto updateRoom(
                        Long roomId,
                        RoomRequestDto request) {
                Room room = roomRepository.findById(roomId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Room not found with id: " + roomId));

                Long requestedBuildingId = request.getBuildingId();
                Long currentBuildingId = room.getBuilding().getBuildingId();

                if (!currentBuildingId.equals(requestedBuildingId)) {
                        Building requestedBuilding = buildingRepository
                                        .findById(requestedBuildingId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Building not found with id: "
                                                                        + requestedBuildingId));
                        room.setBuilding(requestedBuilding);
                }

                boolean roomIdentityChanged = !currentBuildingId.equals(requestedBuildingId)
                                || !room.getRoomNumber()
                                                .equals(request.getRoomNumber());

                if (roomIdentityChanged
                                && roomRepository
                                                .existsByBuildingBuildingIdAndRoomNumber(
                                                                requestedBuildingId,
                                                                request.getRoomNumber())) {
                        throw new IllegalStateException(
                                        "Room already exists in building");
                }

                room.setRoomNumber(request.getRoomNumber());
                room.setSharingType(request.getSharingType());
                room.setRoomType(request.getRoomType());
                room.setRoomStatus(request.getRoomStatus());

                return mapToResponse(roomRepository.save(room));
        }

        @Override
        @Transactional
        public void deleteRoom(Long roomId) {
                Room room = roomRepository.findById(roomId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Room not found with id: " + roomId));

                if (tenantRepository.countByRoomRoomId(roomId) > 0) {
                        throw new IllegalStateException(
                                        "Cannot delete room. Tenants are assigned to this room.");
                }

                roomRepository.delete(room);
        }

        @Override
        @Transactional(readOnly = true)
        public List<RoomResponseDto> getRoomsByBuilding(Long buildingId) {
                return roomRepository.findByBuildingBuildingId(buildingId)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        private RoomResponseDto mapToResponse(Room room) {
                return RoomResponseDto.builder()
                                .roomId(room.getRoomId())
                                .buildingId(room.getBuilding().getBuildingId())
                                .buildingName(room.getBuilding().getBuildingName())
                                .roomNumber(room.getRoomNumber())
                                .sharingType(room.getSharingType())
                                .roomType(room.getRoomType())
                                .capacity(getCapacity(room.getSharingType()))
                                .roomStatus(room.getRoomStatus())
                                .build();
        }

        private Integer getCapacity(SharingType sharingType) {
                return switch (sharingType) {
                        case SINGLE -> 1;
                        case DOUBLE -> 2;
                        case TRIPLE -> 3;
                        case FOUR -> 4;
                };
        }
}
