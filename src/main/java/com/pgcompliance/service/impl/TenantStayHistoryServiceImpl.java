package com.pgcompliance.service.impl;

import com.pgcompliance.dto.TenantStayHistoryDto;
import com.pgcompliance.service.TenantStayHistoryService;
import com.pgcompliance.service.BuildingAccessService;
import com.pgcompliance.entity.RoomTransferHistory;
import com.pgcompliance.entity.Tenant;
import com.pgcompliance.entity.TenantStay;
import com.pgcompliance.exception.ResourceNotFoundException;
import com.pgcompliance.repository.RoomTransferHistoryRepository;
import com.pgcompliance.repository.TenantRepository;
import com.pgcompliance.repository.TenantStayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantStayHistoryServiceImpl implements TenantStayHistoryService {
    private final TenantRepository tenantRepository;
    private final TenantStayRepository stayRepository;
    private final RoomTransferHistoryRepository transferRepository;
    private final BuildingAccessService buildingAccessService;

    @Override
    @Transactional(readOnly = true)
    public List<TenantStayHistoryDto> getHistory(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant not found with id: " + tenantId));

        // Building staff may access histories only for residents currently
        // belonging to their assigned building, matching existing tenant access.
        if (tenant.getRoom() == null) {
            throw new ResourceNotFoundException("Tenant has no assigned room");
        }
        buildingAccessService.validateReadAccess(
                tenant.getRoom().getBuilding().getBuildingId());

        boolean superAdmin = buildingAccessService.isSuperAdmin();
        Long allowedBuildingId = superAdmin
                ? null : buildingAccessService.getAccessibleBuildingId();

        return stayRepository
                .findByTenantTenantIdOrderByJoiningDateDescStayIdDesc(tenantId)
                .stream()
                // Do not expose historical rooms in other buildings to
                // building-level staff.
                .filter(stay -> superAdmin || stay.getRoom()
                        .getBuilding().getBuildingId().equals(allowedBuildingId))
                .map(stay -> toDto(stay, allowedBuildingId))
                .toList();
    }

    private TenantStayHistoryDto toDto(TenantStay stay, Long allowedBuildingId) {
        List<TenantStayHistoryDto.Transfer> transfers = transferRepository
                .findByStayStayIdOrderByTransferredAtAscTransferIdAsc(stay.getStayId())
                .stream()
                .filter(t -> allowedBuildingId == null ||
                        (sameBuilding(t, allowedBuildingId)))
                .map(t -> new TenantStayHistoryDto.Transfer(
                        t.getTransferId(),
                        t.getFromRoom().getRoomId(),
                        t.getFromRoom().getRoomNumber(),
                        t.getToRoom().getRoomId(),
                        t.getToRoom().getRoomNumber(),
                        t.getTransferredAt()))
                .toList();

        return new TenantStayHistoryDto(
                stay.getStayId(),
                stay.getJoiningDate(),
                stay.getLeavingDate(),
                stay.getStayStatus().name(),
                stay.getRoom().getRoomId(),
                stay.getRoom().getRoomNumber(),
                stay.getRoom().getBuilding().getBuildingName(),
                stay.getStayStatus() == TenantStay.StayStatus.COMPLETED
                        && stay.getLeavingDate() == null,
                transfers
        );
    }

    private boolean sameBuilding(RoomTransferHistory transfer, Long buildingId) {
        return buildingId.equals(transfer.getFromRoom().getBuilding().getBuildingId())
                && buildingId.equals(transfer.getToRoom().getBuilding().getBuildingId());
    }
}
