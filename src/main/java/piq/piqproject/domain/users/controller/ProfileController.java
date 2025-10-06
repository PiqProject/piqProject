package piq.piqproject.domain.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.service.ProfileService;

/**
 * ProfileController는 사용자 프로필 관련 API 엔드포인트를 담당합니다.
 * 모든 엔드포인트는 인증된 사용자 본인만 접근 가능합니다.
 */
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
}