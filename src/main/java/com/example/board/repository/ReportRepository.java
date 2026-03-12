package com.example.board.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.board.entity.BoardReport;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<BoardReport, Long> {
    // [조회용] 모든 신고 내역을 최신순으로 가져오기
    List<BoardReport> findAllByOrderByCreatedAtDesc();

    // [중요] 실제 '신고 테이블'에서 중복 신고 여부를 확인하는 로직
    boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);


}
