package com.example.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.admin.dto.settlement.SettlementDashboardResponse;

@FeignClient(name = "pay-service", url = "http://localhost/msa/pay")
public interface PayClient {
	@GetMapping("/api/pay/dashboard")
	SettlementDashboardResponse getDashboardData(@RequestParam("yearMonth") String yearMonth);
}
