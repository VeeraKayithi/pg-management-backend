package com.pgcompliance.repository;

import com.pgcompliance.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface RoomRepository
        extends JpaRepository<Room, Long> {

    // Call only inside an active @Transactional method.
    // PostgreSQL SELECT ... FOR UPDATE serializes assignments to a room.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.roomId = :roomId")
    Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);

    boolean existsByBuildingBuildingId(Long buildingId);

    boolean existsByBuildingBuildingIdAndRoomNumber(
            Long buildingId,
            String roomNumber);

    List<Room> findByBuildingBuildingId(
            Long buildingId);

    List<Room> findAllByOrderByBuildingBuildingIdAscRoomNumberAsc();
}