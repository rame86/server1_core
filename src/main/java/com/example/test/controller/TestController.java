package com.example.test.controller;

/*
[Restful 방식]
	의미	 	http 메소드
	Create	POST(*)
	Read	GET(*)
	Update	PUT
	Delete	DELETE
	
	(*)표준

[ 기존 URL과 Restful 비교 ]

` 게시판목록보기	/board/getBoardList				/board			GET
` 게시글입력화면	/board/insertBoard				/board/write	GET
` 게시글입력(작성)	/board/saveBoard				/board/write	POST
` 게시글상세보기	/board/getBoard?seq=글번호		/board/글번호		GET
` 게시글수정		/board/updateBoard?seq=글번호		/board/글번호		PUT
` 게시글삭제		/board/deleteBoard?seq=글번호		/board/글번호		DELETE




*/



import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.test.dto.TestDto;
import com.example.test.service.TestService;

@RestController
public class TestController {

    @Autowired
    private TestService testService;

    // ---------------------------------------------------------------

    // 1. 모든 모드에서 접근 가능해야 하는 경로 (/, /event)
    @GetMapping("/")
    public String mainPage() {
        return "Main Page: 접근 성공!";
    }

    @GetMapping("/event")
    public String eventPage() {
        return "Event Page: 누구나 볼 수 있습니다.";
    }

    // 2. 인증이 필요한 경로 (보안 모드 확인용)
    @GetMapping("/api/private")
    public Map<String, String> privateApi() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Success");
        response.put("message", "인증된 사용자만 볼 수 있는 데이터입니다.");
        return response;
    }

    // -------------------------------------------------------------

    @GetMapping("/dbtest")
    public List<TestDto> getTestList() {
        // 비즈니스 로직 호출 및 데이터 반환
        return testService.getAllTestData();
    }

}