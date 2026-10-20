package com.pgcompliance.service.impl;

import com.pgcompliance.dto.BuildingRequestDto;
import com.pgcompliance.dto.BuildingResponseDto;
import com.pgcompliance.entity.Building;
import com.pgcompliance.exception.ResourceNotFoundException;
import com.pgcompliance.repository.BuildingRepository;
import com.pgcompliance.repository.RoomRepository;
import com.pgcompliance.service.BuildingAccessService;
import com.pgcompliance.service.BuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingServiceImpl implements BuildingService {

  private final BuildingRepository buildingRepository;
  private final RoomRepository roomRepository;
  private final BuildingAccessService buildingAccessService;

  @Override
  public BuildingResponseDto createBuilding(BuildingRequestDto request) {

    /*
     * Only Super Admin can create a new building.
     */
    buildingAccessService.validateSuperAdminAccess();

    Building building = Building.builder()
        .buildingName(request.getBuildingName())
        .buildingType(request.getBuildingType())
        .address(request.getAddress())
        .city(request.getCity())
        .createdAt(LocalDateTime.now())
        .build();

    Building savedBuilding = buildingRepository.save(building);

    return mapToResponse(savedBuilding);
  }

  @Override
  public List<BuildingResponseDto> getAllBuildings() {

    /*
     * Super Admin can see every building.
     */
    if (buildingAccessService.isSuperAdmin()) {

      return buildingRepository.findAll()
          .stream()
          .map(this::mapToResponse)
          .toList();
    }

    /*
     * BUILDING_ADMIN and SUPERVISOR should only receive
     * their assigned building.
     */
    Long buildingId = buildingAccessService.getAccessibleBuildingId();

    /*
     * Also confirms that this role is actually allowed
     * to read building data.
     */
    buildingAccessService.validateReadAccess(buildingId);

    Building building = buildingRepository.findById(buildingId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Assigned building not found with id: "
                + buildingId));

    return List.of(mapToResponse(building));
  }

  @Override
  public BuildingResponseDto getBuildingById(Long id) {

    /*
     * ADMIN
     * -> any building
     *
     * BUILDING_ADMIN
     * -> assigned building only
     *
     * SUPERVISOR
     * -> assigned building only
     */
    buildingAccessService.validateReadAccess(id);

    Building building = buildingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Building not found with id: " + id));

    return mapToResponse(building);
  }

  @Override
  public BuildingResponseDto updateBuilding(
      Long id,
      BuildingRequestDto request) {

    /*
     * ADMIN can update any building.
     *
     * BUILDING_ADMIN can update only
     * the assigned building.
     *
     * SUPERVISOR cannot update.
     */
    buildingAccessService.validateWriteAccess(id);

    Building building = buildingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Building not found with id: " + id));

    building.setBuildingName(request.getBuildingName());
    building.setBuildingType(request.getBuildingType());
    building.setAddress(request.getAddress());
    building.setCity(request.getCity());

    Building updatedBuilding = buildingRepository.save(building);

    return mapToResponse(updatedBuilding);
  }

  @Override
  public void deleteBuilding(Long id) {

    /*
     * Deleting an entire building is a Super Admin
     * operation only.
     */
    buildingAccessService.validateSuperAdminAccess();

    Building building = buildingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Building not found with id: " + id));

    /*
     * Preserve your existing protection:
     * a building containing rooms cannot be deleted.
     */
    if (roomRepository.existsByBuildingBuildingId(id)) {

      throw new RuntimeException(
          "Cannot delete building. Rooms are assigned to this building.");
    }

    buildingRepository.delete(building);
  }

  private BuildingResponseDto mapToResponse(Building building) {

    return BuildingResponseDto.builder()
        .buildingId(building.getBuildingId())
        .buildingName(building.getBuildingName())
        .buildingType(building.getBuildingType())
        .address(building.getAddress())
        .city(building.getCity())
        .build();
  }
}