package piq.piqproject.domain.alarms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import piq.piqproject.domain.alarms.dto.request.DeviceTokenRequestDto;
import piq.piqproject.domain.alarms.service.AlarmService;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    /**
     * 클라이언트로부터 FCM 디바이스 토큰을 받아 등록합니다.
     * 
     * @param userEntity @AuthenticationPrincipal을 통해 주입된 인증된 사용자
     * @param requestDto 토큰 및 디바이스 정보
     * @return 성공 응답
     */
    @PostMapping("/tokens")
    public ResponseEntity<String> registerDeviceToken(
            @AuthenticationPrincipal UserEntity userEntity,
            @Valid @RequestBody DeviceTokenRequestDto requestDto) {
        alarmService.registerToken(userEntity.getId(), requestDto);
        return ResponseEntity.ok("디바이스 토큰이 성공적으로 등록되었습니다.");
    }

    /**
     * 클라이언트로부터 FCM 디바이스 토큰을 제거합니다. (로그아웃, 앱 제거, 토큰 만료 시 호출)
     * 
     * @param userEntity @AuthenticationPrincipal을 통해 주입된 인증된 사용자
     * @return 성공 응답
     */
    @PostMapping("/tokens/delete")
    public ResponseEntity<String> deleteDeviceToken(
            @AuthenticationPrincipal UserEntity userEntity) {
        alarmService.deleteToken(userEntity.getId());
        return ResponseEntity.ok("디바이스 토큰이 성공적으로 제거되었습니다.");
    }
}
