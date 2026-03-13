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
            // boardId가 Record 형태라면 message.boardId()를, 일반 객체라면 getBoardId()를 사용하세요.
            if (message.boardId() != null) {
                // 주의: approveReport가 아닌 hideBoardByMessage를 호출해야 무한루프를 방지합니다.
                boardService.hideBoardByMessage(message.boardId());
                log.info(">>>> [RabbitMQ 처리완료] 게시글 ID {} 숨김 처리 완료", message.boardId());
            }
        } catch (Exception e) {
            log.error(">>>> [RabbitMQ 에러] 승인 메시지 처리 중 오류 발생: {}", e.getMessage());
        }
    }
}