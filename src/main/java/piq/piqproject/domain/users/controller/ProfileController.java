package piq.piqproject.domain.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.users.dto.request.UserIdealRequestDto;
import piq.piqproject.domain.users.dto.request.UserInterestRequestDto;
import piq.piqproject.domain.users.dto.request.UserIntroduceRequestDto;
import piq.piqproject.domain.users.dto.request.UserLocationRequestDto;
import piq.piqproject.domain.users.dto.request.UserProfileInitRequestDto;
import piq.piqproject.domain.users.dto.request.UserScoreRequestDto;
import piq.piqproject.domain.users.dto.request.UserTraitRequestDto;
import piq.piqproject.domain.users.dto.response.UserIdealResponseDto;
import piq.piqproject.domain.users.dto.response.UserInterestResponseDto;
import piq.piqproject.domain.users.dto.response.UserScoreResponseDto;
import piq.piqproject.domain.users.dto.response.UserTraitResponseDto;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.service.ProfileService;

/**
 * ProfileController는 사용자 프로필 관련 API 엔드포인트를 담당합니다.
 * 모든 엔드포인트는 인증된 사용자 본인만 접근 가능합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class ProfileController {

    private final ProfileService profileService;

    /**
     * 현재 로그인된 사용자의 관심사를 생성 및 수정하는 API입니다.
     * 
     * @param userEntity             @AuthenticationPrincipal을 통해 주입된 현재 인증된 사용자 엔티티
     * @param UserInterestRequestDto 유저가 선택한 관심사 리스트를 담은 request dto
     * @return 사용자의 관심사 리스트
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PutMapping("/me/interests")
    public ResponseEntity<ListResponseDto<UserInterestResponseDto>> upsertUserInterests(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UserInterestRequestDto userInterestRequestDto) {
        log.info("Request to upsert(insert + update) user interests. User: {}", user.getEmail());
        return ResponseEntity.ok(profileService.upsertUserInterests(user, userInterestRequestDto));
    }

    /**
     * 현재 로그인된 사용자의 실제 특성을 생성 및 수정하는 API입니다.
     *
     * @param user                @AuthenticationPrincipal을 통해 주입된 현재 인증된 사용자 엔티티
     * @param userTraitRequestDto 사용자가 선택한 자신의 특성 리스트를 담은 request DTO
     * @return 사용자의 특성 리스트
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PutMapping("/me/traits")
    public ResponseEntity<ListResponseDto<UserTraitResponseDto>> upsertUserTraits(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UserTraitRequestDto userTraitRequestDto) {
        log.info("Request to upsert user traits. User: {}", user.getEmail());
        return ResponseEntity.ok(profileService.upsertUserTraits(user, userTraitRequestDto));
    }

    /**
     * 현재 로그인된 사용자의 이상형을 생성 및 수정하는 API입니다.
     * 
     * @param userEntity          @AuthenticationPrincipal을 통해 주입된 현재 인증된 사용자 엔티티
     * @param UserIdealRequestDto 유저가 선택한 이상형 리스트를 담은 request dto
     * @return 사용자의 이상형 리스트
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PutMapping("/me/ideals")
    public ResponseEntity<ListResponseDto<UserIdealResponseDto>> upsertUserIdeals(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UserIdealRequestDto userIdealRequestDto) {
        log.info("Request to upsert(insert + update) user ideals. User: {}", user.getEmail());
        return ResponseEntity.ok(profileService.upsertUserIdeals(user, userIdealRequestDto));
    }

    /**
     * 매칭이후 상대방의 매너 점수를 매기는 API입니다.
     * 
     * @param userEntity          @AuthenticationPrincipal을 통해 주입된 현재 인증된 사용자 엔티티
     * @param UserScoreRequestDto 매칭 상대에 대한 점수를 담은 request dto
     * @return UserScoreResponseDto (점수를 매긴 유저 id, 상대 user 정보, 매긴 점수)
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PutMapping("/score")
    public ResponseEntity<UserScoreResponseDto> scoreUser(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UserScoreRequestDto userScoreRequestDto) {
        log.info("Request to score user User: {}", user.getEmail());
        return ResponseEntity.ok(profileService.scoreUser(user, userScoreRequestDto));
    }

    /**
     * 사용자의 거주지(주소)를 변경합니다.
     * 입력된 주소를 기반으로 위도/경도를 자동으로 계산하여 저장합니다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PutMapping("/me/location")
    public ResponseEntity<String> updateUserLocation(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UserLocationRequestDto requestDto) {

        profileService.updateUserLocation(user, requestDto);

        return ResponseEntity.ok("주소 변경이 완료되었습니다.");
    }

    /**
     * [신규 회원] 최초 프로필 정보 입력
     * 이 API를 호출하면 GUEST 권한이 USER 권한으로 변경됩니다.
     */
    @PutMapping("/me/profile/init")
    public ResponseEntity<String> initUserProfile(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UserProfileInitRequestDto request) {

        log.info("Request to init profile. User: {}", user.getEmail());
        profileService.initUserProfile(user, request);

        return ResponseEntity.ok("프로필 입력이 완료되었습니다. 이제 서비스를 이용하실 수 있습니다.");
    }

    /**
     * 사용자의 자기소개를 수정합니다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PutMapping("/me/introduce")
    public ResponseEntity<String> updateIntroduce(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UserIntroduceRequestDto requestDto) {

        profileService.updateUserIntroduce(user.getId(), requestDto);

        return ResponseEntity.ok("자기소개 변경이 완료되었습니다.");
    }
}