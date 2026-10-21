package com.pgcompliance.controller;

import com.pgcompliance.dto.TenantStayHistoryDto;
import com.pgcompliance.service.TenantStayHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantStayHistoryController {
    private final TenantStayHistoryService historyService;

    // Authorization: GET /api/v1/tenants/** is restricted to management
    // roles by SecurityConfig; service additionally enforces building scope.
    @GetMapping("/{tenantId}/stays")
    public List<TenantStayHistoryDto> getStays(@PathVariable Long tenantId) {
        return historyService.getHistory(tenantId);
    }
}
