package com.pgcompliance.repository;

import com.pgcompliance.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository
        extends JpaRepository<Room, Long> {

    boolean existsByBuildingBuildingId(Long buildingId);

    boolean existsByBuildingBuildingIdAndRoomNumber(
            Long buildingId,
            String roomNumber);

    List<Room> findByBuildingBuildingId(
            Long buildingId);

    List<Room> findAllByOrderByBuildingBuildingIdAscRoomNumberAsc();
}