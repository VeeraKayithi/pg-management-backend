package com.pgcompliance.service;

import com.pgcompliance.dto.TenantStayHistoryDto;
import java.util.List;

public interface TenantStayHistoryService {
    List<TenantStayHistoryDto> getHistory(Long tenantId);
}
