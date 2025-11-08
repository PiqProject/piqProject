package piq.piqproject.domain.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.list.ListResponseDto;
import piq.piqproject.domain.users.dto.request.UserIdealRequestDto;
import piq.piqproject.domain.users.dto.request.UserInterestRequestDto;
import piq.piqproject.domain.users.dto.request.UserScoreRequestDto;
import piq.piqproject.domain.users.dto.response.UserIdealResponseDto;
import piq.piqproject.domain.users.dto.response.UserInterestResponseDto;
import piq.piqproject.domain.users.dto.response.UserScoreResponseDto;
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
     * 현재 로그인된 사용자의 프로필 음성을 업로드(또는 교체)합니다.
     * 
     * @param userPrincipal 현재 사용자 정보
     * @param voiceFile     'voiceFile'이라는 key로 전송된 multipart/form-data 형식의 음성 파일
     * @return 성공 시 200 OK
     */
    @PostMapping("/me/voice")
    public ResponseEntity<String> uploadMyVoice(
            @AuthenticationPrincipal UserEntity userPrincipal,
            @RequestPart("voiceFile") MultipartFile voiceFile) {

        // 서비스에 현재 사용자 ID와 음성 파일을 전달
        profileService.uploadVoice(userPrincipal.getId(), voiceFile);

        return ResponseEntity.ok("Voice uploaded successfully.");
    }

    /**
     * 현재 로그인된 사용자의 프로필 음성을 삭제합니다.
     *
     * @param userPrincipal 현재 사용자 정보
     * @return 성공 시 200 OK
     */
    @PostMapping("/me/voice/delete")
    public ResponseEntity<String> deleteMyVoice(
            @AuthenticationPrincipal UserEntity userEntity) {

        profileService.deleteVoice(userEntity.getId());

        return ResponseEntity.ok("Voice deleted successfully.");
    }

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
            @Valid @RequestBody UserInterestRequestDto userInterestRequestDto) 
    {
        log.info("Request to upsert(insert + update) user interests. User: {}", user.getEmail());
        return ResponseEntity.ok(profileService.upsertUserInterests(user, userInterestRequestDto));
    }

    /**
     * 현재 로그인된 사용자의 이상형을 생성 및 수정하는 API입니다.
     * 
     * @param userEntity             @AuthenticationPrincipal을 통해 주입된 현재 인증된 사용자 엔티티
     * @param UserIdealRequestDto 유저가 선택한 이상형 리스트를 담은 request dto
     * @return 사용자의 이상형 리스트
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PutMapping("/me/ideals")
    public ResponseEntity<ListResponseDto<UserIdealResponseDto>> upsertUserIdeals(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UserIdealRequestDto userIdealRequestDto) 
    {
        log.info("Request to upsert(insert + update) user ideals. User: {}", user.getEmail());
        return ResponseEntity.ok(profileService.upsertUserIdeals(user, userIdealRequestDto));
    }

    /**
     * 매칭이후 상대방의 매너 점수를 매기는 API입니다. 
     * 
     * @param userEntity             @AuthenticationPrincipal을 통해 주입된 현재 인증된 사용자 엔티티
     * @param UserScoreRequestDto 매칭 상대에 대한 점수를 담은 request dto
     * @return UserScoreResponseDto (점수를 매긴 유저 id, 상대 user 정보, 매긴 점수)
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PutMapping("/score")
    public ResponseEntity<UserScoreResponseDto> scoreUser(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UserScoreRequestDto userScoreRequestDto) 
    {
        log.info("Request to score user User: {}", user.getEmail());
        return ResponseEntity.ok(profileService.scoreUser(user, userScoreRequestDto));
    }
}