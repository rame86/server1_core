package com.example.board.repository;

import com.example.board.entity.ReportComment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {
    // 댓글 ID와 회원 ID로 이미 신고했는지 확인
    boolean existsByCommentIdAndMemberId(Long commentId, Long memberId);

    // [추가] 전달받은 댓글 ID 리스트에 해당하는 모든 신고 내역을 가져옵니다.
    List<ReportComment> findByCommentIdIn(List<Long> commentIds);

    void deleteByCommentId(Long commentId);
    void deleteByCommentIdIn(List<Long> commentIds);
}
