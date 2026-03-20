package com.example.board.message;

import com.example.admin.dto.BoardReportMessageDTO;
import com.example.board.service.BoardService;
import com.example.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardMessageListener {

    private final BoardService boardService;

    // 관리자 서비스에서 보낸 승인 메시지를 수신 (큐 이름은 Config 상수를 쓰는 것이 유지보수에 좋습니다)
    @RabbitListener(queues = RabbitMQConfig.BOARD_REPORT_APPROVE_QUEUE_NAME)
    public void receiveApproveMessage(BoardReportMessageDTO message) {
        log.info(">>>> [RabbitMQ 수신] 신고 승인 메시지 도착: {}", message);
        
        try {
        // message.boardId() 또는 message.getBoardId() 중 DTO 구조에 맞는 것을 사용하세요.
        Long targetId = message.boardId(); 
        
        if (targetId != null) {
            boardService.hideBoardByMessage(targetId);
            log.info(">>>> [RabbitMQ 처리완료] 게시글 ID {} 숨김 처리 완료", targetId);
        }
    } catch (Exception e) {
        log.error(">>>> [RabbitMQ 에러] 처리 중 오류: {}", e.getMessage());
    }
}
}