package com.example.board.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.example.board.entity.Board;
import com.example.board.entity.LikeBoard;
import com.example.board.entity.Comment;

public interface BoardRepository extends JpaRepository<Board, Long> {
    // 게시글 레포지토리
    List<Board> findAllByOrderByCreatedAtDesc();
    // 2. 특정 카테고리를 선택했을 때용 (카테고리 조건 + 최신순 정렬)
    List<Board> findByCategoryOrderByCreatedAtDesc(String category);
    List<Board> findByCategoryAndArtistIdOrderByCreatedAtDesc(String category, Long artistId);

   // 좋아요 레포지토리
    interface LikeRepository extends JpaRepository<LikeBoard, Long> {
        boolean existsByBoardIdAndMemberId(Long boardId, Long memberId);
        void deleteByBoardIdAndMemberId(Long boardId, Long memberId);
    }
   
    // 3. 댓글 리포지토리
    interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByBoardIdOrderByCreatedAtDesc(Long boardId);

    }
