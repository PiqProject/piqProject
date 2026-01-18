package piq.piqproject.domain.recommendations.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import piq.piqproject.common.annotation.RequireActiveUser;
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
     * [선택 A] 기본 거리 기반 추천 받기 (무료)
     * - 오늘 추천을 아직 안 받았다면 -> 거리 기반으로 생성 후 반환
     * - 이미 받았다면 -> 기존 내역 반환
     */
    @GetMapping("/daily")
    @RequireActiveUser
    public ResponseEntity<ListResponseDto<RecommendedUserResponseDto>> getDailyRecommendations(
            @AuthenticationPrincipal UserEntity user) {

        // 1. 실제 비즈니스 로직은 Service 계층에 모두 위임합니다.
        // 컨트롤러는 단지 요청을 받고, 적절한 서비스 메서드를 호출하며, 결과를 반환하는 역할만 수행합니다.
        List<RecommendedUserResponseDto> recommendations = dailyRecommendationService.getDailyRecommendations(user,
                false);

        // 2. 서비스로부터 받은 결과를 ResponseEntity에 담아 클라이언트에게 반환합니다.
        // ResponseEntity.ok()는 HTTP 200 OK 상태와 응답 본문을 함께 설정해줍니다.
        return ResponseEntity.ok(ListResponseDto.from(recommendations));
    }

    /**
     * [선택 B] 프리미엄 이상형 추천 받기 (유료)
     * - 오늘 추천을 아직 안 받았다면 -> 포인트 차감 후 이상형 기반 생성
     * - 이미 받았다면 -> (정책에 따라) 기존 내역 반환 또는 에러 처리
     * (여기서는 사용자가 실수로 눌렀을 수도 있으니 기존 내역을 보여주는 것으로 구현)
     */
    @GetMapping("/daily/premium")
    @RequireActiveUser
    public ResponseEntity<ListResponseDto<RecommendedUserResponseDto>> getPremiumRecommendations(
            @AuthenticationPrincipal UserEntity user) {

        // true = 프리미엄 요청
        List<RecommendedUserResponseDto> recommendations = dailyRecommendationService.getDailyRecommendations(user,
                true);
        return ResponseEntity.ok(ListResponseDto.from(recommendations));
    }
}