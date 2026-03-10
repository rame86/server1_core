// ... 기존 import 생략
package com.example.board.service;

import com.example.board.entity.Board;
import com.example.board.entity.LikeBoard;
import com.example.board.entity.Comment;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.LikeRepository;
import com.example.board.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final LikeRepository likeRepository;       
    private final CommentRepository commentRepository; 

    // ... 기존 메서드들 (getBoardList, writeBoard 등) 유지

    // [추가] 좋아요 토글 로직
    @Transactional
    public int toggleLike(Long boardId, Long memberId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));

        if (likeRepository.existsByBoardIdAndMemberId(boardId, memberId)) {
            likeRepository.deleteByBoardIdAndMemberId(boardId, memberId);
            board.setLikeCount(Math.max(0, board.getLikeCount() - 1));
        } else {
           // 2. 좋아요가 없다면: 데이터 추가 + 숫자 증가
            likeRepository.save(LikeBoard.builder()
                    .boardId(boardId)
                    .memberId(memberId)
                    .build());
            board.setLikeCount(board.getLikeCount() + 1);
        }
        // 변경된 좋아요 수를 반환 (프론트 반영용)
        return board.getLikeCount();
    }

    // [추가] 댓글 작성 로직
    @Transactional
    public void addComment(Long boardId, Long memberId, String content) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        // 1. 댓글 엔티티 생성 및 저장 (댓글 내용이 추가됨)
        Comment comment = Comment.builder()
                .boardId(boardId)
                .memberId(memberId)
                .content(content)
                .build();
        
        commentRepository.save(comment);

        // 2. 게시글의 댓글 수 증가 (목록 화면을 위해 숫자 업데이트)
        board.setCommentCount(board.getCommentCount() + 1);

        return board.getCommentCount();
    }
}