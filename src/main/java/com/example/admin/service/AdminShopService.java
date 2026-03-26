package com.example.admin.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.admin.dto.approval.ShopResultDTO;
import com.example.admin.entity.Approval;
import com.example.admin.repository.ApprovalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminShopService {
	
	private final ApprovalRepository approvalRepository;
	private final ObjectMapper objectMapper;
	
	// 굿즈(Shop) 승인 대기 목록
	public List<ShopResultDTO> getPendingShopList(String category, String status) {
		List<Approval> entityList = approvalRepository.findByCategoryAndStatus(category, status);
		return entityList.stream().map(entity -> {
			try {
				// contentJson에 저장된 상세 정보를 ShopResultDTO로 복원
				ShopResultDTO dto = objectMapper.readValue(entity.getContentJson(), ShopResultDTO.class);

				// 엔티티의 기본 필드(ID, 상태, 날짜 등)로 덮어쓰기 (최신성 유지)
				return ShopResultDTO.builder().approvalId(entity.getApprovalId()).goodsId(entity.getTargetId())
						.requesterId(entity.getArtistId()).requesterName(entity.getRequesterName())
						.goodsName(entity.getTitle())
						.goodsType(entity.getSubCategory() != null ? entity.getSubCategory()
								: (dto != null ? dto.getGoodsType() : "UNKNOWN"))
						.description(entity.getDescription())
						.price(entity.getPrice() != null ? entity.getPrice().intValue()
								: (dto != null ? dto.getPrice() : 0))
						.color(dto != null ? dto.getColor() : null).size(dto != null ? dto.getSize() : null)
						.stockQuantity(dto != null ? dto.getStockQuantity() : 0).imageUrl(entity.getImageUrl())
						.status(entity.getStatus()).createdAt(entity.getCreatedAt().toString())
						.rejectionReason(entity.getRejectionReason()).build();
			} catch (Exception e) {
				log.error("Failed to parse shop approval JSON: {}", e.getMessage());
				// 파싱 실패 시 기본 정보라도 반환
				return ShopResultDTO.builder().approvalId(entity.getApprovalId()).goodsName(entity.getTitle())
						.status(entity.getStatus()).build();
			}
		}).collect(Collectors.toList());
	}

}
