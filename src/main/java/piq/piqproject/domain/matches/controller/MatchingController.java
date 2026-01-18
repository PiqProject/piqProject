package piq.piqproject.domain.matches.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import piq.piqproject.common.annotation.RequireActiveUser;
import piq.piqproject.domain.matches.dto.request.MatchingRequestDto;
import piq.piqproject.domain.matches.dto.request.UpdateMatchingRequestDto;
import piq.piqproject.domain.matches.dto.response.ContactExchangeResponseDto;
import piq.piqproject.domain.matches.dto.response.MatchingResponseDto;
import piq.piqproject.domain.matches.service.MatchingService;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matches")
public class MatchingController {

    private final MatchingService matchingService;

    /**
     * 매칭 요청 API
     * 
     * @param requestDto
     * @return 생성된 매칭 정보
     */
    @PostMapping
    @RequireActiveUser
    public ResponseEntity<MatchingResponseDto> createMatch(@AuthenticationPrincipal UserEntity userEntity,
            @Valid @RequestBody MatchingRequestDto requestDto) {
        MatchingResponseDto response = matchingService.createMatch(userEntity.getId(), requestDto);

        // 생성된 리소스의 URI를 Location 헤더에 담아 201 Created 응답 반환
        URI location = URI.create(String.format("/api/v1/matches/%d", response.getMatchId()));
        return ResponseEntity.created(location).body(response);
    }

    /**
     * 매칭 수락/거절 API
     * 
     * @param matchId    URL 경로에서 받아오는 매칭 ID
     * @param userEntity 현재 로그인된 사용자 정보
     * @param requestDto 요청 본문 (ACCEPT / REJECT)
     * @return 업데이트된 매칭 정보
     */
    @PatchMapping("/{matchId}")
    @RequireActiveUser
    public ResponseEntity<MatchingResponseDto> updateMatch(
            @PathVariable("matchId") Long matchId,
            @AuthenticationPrincipal UserEntity userEntity,
            @Valid @RequestBody UpdateMatchingRequestDto requestDto) {

        Long currentUserId = userEntity.getId();
        MatchingResponseDto response = matchingService.updateMatchStatus(matchId, currentUserId, requestDto);

        return ResponseEntity.ok(response);
    }

    /**
     * 내가 보낸 매칭 요청 목록 조회 API
     */
    @GetMapping("/sent")
    public ResponseEntity<Page<MatchingResponseDto>> getSentMatches(
            @AuthenticationPrincipal UserEntity userEntity,
            @PageableDefault(size = 6, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long currentUserId = userEntity.getId();
        Page<MatchingResponseDto> result = matchingService.getSentMatches(currentUserId, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * 내가 받은 매칭 요청 목록 조회 API
     */
    @GetMapping("/received")
    public ResponseEntity<Page<MatchingResponseDto>> getReceivedMatches(
            @AuthenticationPrincipal UserEntity userEntity,
            @PageableDefault(size = 6, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long currentUserId = userEntity.getId();
        Page<MatchingResponseDto> result = matchingService.getReceivedMatches(currentUserId, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * 매칭 성공 후 연락처 교환 API
     */
    @GetMapping("/{matchId}/exchange-contact")
    @RequireActiveUser
    public ResponseEntity<ContactExchangeResponseDto> exchangeContact(
            @PathVariable("matchId") Long matchId,
            @AuthenticationPrincipal UserEntity userEntity) {

        Long currentUserId = userEntity.getId();
        ContactExchangeResponseDto response = matchingService.exchangeContact(matchId, currentUserId);

        return ResponseEntity.ok(response);
    }

}