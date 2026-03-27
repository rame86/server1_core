package com.example.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.admin.service.AdminArtistService;
import com.example.admin.service.AdminEventService;
import com.example.admin.service.AdminUserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashBoardController {
	
	private final AdminUserService adminUserService;
    private final AdminArtistService adminArtistService;
    private final AdminEventService adminEventService;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> data = new HashMap<>();

        data.put("totalUsers", adminUserService.getUserSummary().getTotalUserCount());
        data.put("totalArtists", adminArtistService.getActiceArtistList("ARTIST", "CONFIRMED").size());
        data.put("userGrowth", adminUserService.userGrowthCounts());

        Map<String, Long> eventStats = adminEventService.getEventCounts();
        data.put("totalEvents", eventStats.get("confirmedCount"));
        data.put("pendingEvents", eventStats.get("pendingCount"));
        data.put("pendingGoods", eventStats.get("shopCount"));
        
        data.put("pendingArtists", adminArtistService.getPendingArtistList("ARTIST", "PENDING").size());
        data.put("pendingReports", 0);

        return ResponseEntity.ok(data);
    }

}
