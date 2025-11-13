package piq.piqproject.domain.dislikes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.domain.dislikes.dto.request.DislikeRequestDto;
import piq.piqproject.domain.dislikes.service.DislikeService;
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * '싫어요' 기능과 관련된 API 요청을 처리하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dislikes")
@RequiredArgsConstructor
public class DislikeController {

    private final DislikeService dislikeService;

    /**
     * 특정 사용자를 '싫어요' 목록에 추가합니다.
     *
     * @param user       현재 로그인하여 '싫어요'를 누르는 주체 (Spring Security가 주입)
     * @param requestDto '싫어요' 대상 사용자의 ID가 포함된 요청 DTO
     * @return 성공 시 HTTP 200 OK 상태 코드와 빈 응답 본문을 반환합니다.
     */
    @PostMapping
    public ResponseEntity<Void> createDislike(
            @AuthenticationPrincipal UserEntity user,
            @RequestBody DislikeRequestDto requestDto) {

        log.info("사용자 ID {} 가 사용자 ID {} 를 '싫어요' 처리합니다.", user.getId(), requestDto.getDislikedUserId());

        // 1. 컨트롤러는 요청을 받아 필요한 데이터를 서비스 계층으로 전달하는 역할만 수행합니다.
        // - 누가 (user)
        // - 누구를 (requestDto.getDislikedUserId())
        dislikeService.createDislike(user, requestDto.getDislikedUserId());

        // 2. 별도의 반환 데이터가 필요 없으므로, HTTP 200 OK 상태만 응답합니다.
        // .build()는 내용 없는 응답을 생성합니다.
        return ResponseEntity.ok().build();
    }
}