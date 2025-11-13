package piq.piqproject.domain.recommendations.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.recommendations.dto.RecommendedUserResponseDto;
import piq.piqproject.domain.recommendations.service.DailyRecommendationService;
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * 사용자 추천 관련 API 요청을 처리하는 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class DailyRecommendationController {

    private final DailyRecommendationService dailyRecommendationService;

    /**
     * 오늘의 추천 사용자 카드 2장을 조회하는 API 엔드포인트입니다.
     * 이 API는 하루 동안 동일한 결과를 반환하는 것을 보장합니다.
     *
     * @param userDetails Spring Security가 주입해주는 현재 인증된 사용자의 정보.
     *                    UserDetails 인터페이스를 통해 사용자의 고유 식별자(username)를 얻습니다.
     * @return 성공 시 HTTP 200 OK 상태 코드와 함께 추천 사용자 정보 DTO 리스트를 반환합니다.
     */
    @GetMapping("/daily")
    public ResponseEntity<ListResponseDto<RecommendedUserResponseDto>> getDailyRecommendations(
            @AuthenticationPrincipal UserEntity user) {

        // 1. 실제 비즈니스 로직은 Service 계층에 모두 위임합니다.
        // 컨트롤러는 단지 요청을 받고, 적절한 서비스 메서드를 호출하며, 결과를 반환하는 역할만 수행합니다.
        List<RecommendedUserResponseDto> recommendations = dailyRecommendationService.getDailyRecommendations(user);

        // 2. 서비스로부터 받은 결과를 ResponseEntity에 담아 클라이언트에게 반환합니다.
        // ResponseEntity.ok()는 HTTP 200 OK 상태와 응답 본문을 함께 설정해줍니다.
        return ResponseEntity.ok(ListResponseDto.from(recommendations));
    }
}