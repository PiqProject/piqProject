package piq.piqproject.domain.verification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.domain.admin.dto.response.UserVerificationResponseDto;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.verification.service.VerifiationService;

/**
 * 일반 유저가 자신의 검증 정보(이미지, 음성)를 조회하는 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerifiationService verifiationService;

    /**
     * 내 검증 목록 조회
     * GET /api/v1/verification/me
     * - 로그인한 유저 자신의 이미지, 음성 검증 내역을 반환합니다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @GetMapping("/me")
    public ResponseEntity<List<UserVerificationResponseDto>> getMyVerifications(
            @AuthenticationPrincipal UserEntity user) {

        log.info("User {} requested own verifications.", user.getEmail());
        return ResponseEntity.ok(verifiationService.getMyVerifications(user.getId()));
    }
}
