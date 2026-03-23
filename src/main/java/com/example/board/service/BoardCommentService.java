package com.example.board.service;

import com.example.board.dto.CommentRequestDTO;
import com.example.board.dto.CommentResponseDTO;
import com.example.board.entity.Board;
import com.example.board.entity.Comment;
import com.example.board.repository.BoardRepository;
import com.example.board.repository.CommentRepository;
import com.example.member.domain.Member;
import com.example.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardCommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    // 댓글 작성
    @Transactional
    public CommentResponseDTO createComment(CommentRequestDTO requestDTO) {
        Board board = boardRepository.findById(requestDTO.getBoardId())
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. ID: " + requestDTO.getBoardId()));

        Comment comment = Comment.builder()
                .boardId(board)
                .memberId(requestDTO.getMemberId())
                .content(requestDTO.getContent())
                .status("ACTIVE") 
                .build();

        Comment savedComment = commentRepository.save(comment);
        board.incrementCommentCount(); 
        
        // [수정] 작성 직후 이름 조회 (Member 엔티티의 필드명 확인: getName vs getUserName)
        Member writer = memberRepository.findById(savedComment.getMemberId()).orElse(null);
        String authorName = (writer != null) ? writer.getName() : "알 수 없는 사용자";

        return convertToCommentDTO(savedComment, authorName);
    }

    // 특정 게시글의 댓글 조회
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getCommentsByBoardId(Long boardId) {
        if (!boardRepository.existsById(boardId)) {
            throw new IllegalArgumentException("해당 게시글이 존재하지 않습니다. ID: " + boardId);
        }
        List<Comment> comments = commentRepository.findByBoardId_BoardIdAndStatusOrderByCreatedAtDesc(boardId, "ACTIVE");
        
        // 1. 댓글 작성자 ID 수집
        Set<Long> memberIds = comments.stream()
                .map(Comment::getMemberId)
                .collect(Collectors.toSet());
        
        // 이름 매핑 정보 가져오기
        Map<Long, String> memberNameMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(
                Member::getMemberId,
                member -> member.getName() != null ? member.getName() : "이름 없음",
                (existing, replacement) -> existing
        ));

        return comments.stream()
            .map(comment -> {
                    String authorName = memberNameMap.getOrDefault(comment.getMemberId(), "User_" + comment.getMemberId());
                    return convertToCommentDTO(comment, authorName);
                })
                .toList();
        }
        // 댓글 수정
        @Transactional
        public CommentResponseDTO updateComment(Long commentId, CommentRequestDTO requestDTO, Long memberId){
           // 1. 전달받은 commentId(12)를 로그로 찍어 확인
        log.info("====> 댓글 수정 서비스 진입 - commentId: {}, memberId: {}", commentId, memberId);

        // 2. 반드시 첫 번째 인자인 commentId로 조회해야 함
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("====> 댓글 조회 실패! DB에 해당 ID가 없음: {}", commentId);
                    return new IllegalArgumentException("해당 댓글이 존재하지 않습니다. ID: " + commentId);
                });

        // 3. 권한 확인 (DB의 member_id와 로그인 세션의 memberId 비교)
        if (!comment.getMemberId().equals(memberId)) {
            log.warn("====> [권한 에러] 작성자({})와 요청자({}) 불일치", comment.getMemberId(), memberId);
            throw new IllegalStateException("해당 댓글을 수정할 권한이 없습니다.");
        }

        // 4. 내용 수정
        comment.updateContent(requestDTO.getContent());

        // 5. 작성자 이름 조회 (Member 엔티티의 PK는 memberId임)
        Member writer = memberRepository.findById(comment.getMemberId()).orElse(null);
        String authorName = (writer != null) ? writer.getName() : "알 수 없는 사용자";

        return convertToCommentDTO(comment, authorName);
    }
        //댓글 삭제 
        @Transactional
        public void deleteComment(Long commentId, Long memberId, String role) {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다. ID: " + commentId));
            
            if (!comment.getMemberId().equals(memberId) && !"ADMIN".equals(role)) {
                throw new IllegalStateException("해당 댓글을 삭제할 권한이 없습니다.");
            }

            Board board = comment.getBoardId();
            commentRepository.delete(comment);
            board.decrementCommentCount(); 
    }

    // Entity -> DTO 변환
    private CommentResponseDTO convertToCommentDTO(Comment comment, String authorName) {
        log.info("==== [최종 데이터 확인] 댓글ID: {}, 작성자명: {} ====", comment.getCommentId(), authorName);
    return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .boardId(comment.getBoardId().getBoardId())
                .memberId(comment.getMemberId())
                .content(comment.getContent())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .authorName(authorName)
                .build();
    }
}