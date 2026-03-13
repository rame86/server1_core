package com.example.board.repository;

import com.example.board.entity.ReportComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {
    // 댓글 ID와 회원 ID로 이미 신고했는지 확인
    boolean existsByCommentIdAndMemberId(Long commentId, Long memberId);
}
