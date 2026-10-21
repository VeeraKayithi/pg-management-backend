package com.pgcompliance.repository;

import com.pgcompliance.entity.RoomTransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomTransferHistoryRepository extends JpaRepository<RoomTransferHistory, Long> {

    List<RoomTransferHistory> findByStayStayIdOrderByTransferredAtAscTransferIdAsc(Long stayId);
}
