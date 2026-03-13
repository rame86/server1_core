package com.example.admin.dto;

import java.util.List;
import java.util.Map;

public record SettlementDashboardResponse(
	Map<String, String> summary, // Redis (상단 카드 4개)
	List<GraphData> chartData, // RDB (그래프 2개)
	List<ArtistSettlement> artistList // RDB (아티스트 상세 내역)
) {}