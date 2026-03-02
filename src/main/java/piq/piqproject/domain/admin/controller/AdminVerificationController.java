package piq.piqproject.domain.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.annotation.AuditLog;
import piq.piqproject.domain.admin.dto.response.UserVerificationResponseDto;
import piq.piqproject.domain.admin.dto.request.VerificationSearchRequestDto;
import piq.piqproject.domain.admin.service.AdminVerificationService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/verification")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    /**
     * 회원 이미지, 음성, 소개글 검증 목록 조회
     */
    @GetMapping("")
    @AuditLog(action = "회원 이미지, 음성, 소개글 검증 목록 조회")
    public ResponseEntity<Page<UserVerificationResponseDto>> getUsers(
            VerificationSearchRequestDto requestDto,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} accessed user verification list. Filter [Type: {}, Status: {}]",
                admin.getEmail(), requestDto.getContentType(), requestDto.getStatus());
        return ResponseEntity.ok(adminVerificationService.getVerifications(requestDto, pageable));
    }

    /**
     * 회원 이미지, 음성 검증 허용
     */
    @PutMapping("/{verificationId}/approve")
    @AuditLog(action = "회원 이미지, 음성 검증 허용")
    public ResponseEntity<String> approveVerification(
            @PathVariable("verificationId") Long verificationId,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} approved verification {}.", admin.getEmail(), verificationId);
        adminVerificationService.approveVerification(verificationId);

        return ResponseEntity.ok("회원 이미지, 음성 검증이 허용되었습니다.");
    }

    /**
     * 회원 이미지, 음성 검증 거절
     */
    @PutMapping("/{verificationId}/reject")
    @AuditLog(action = "회원 이미지, 음성 검증 거절")
    public ResponseEntity<String> rejectVerification(
            @PathVariable("verificationId") Long verificationId,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} rejected verification {}.", admin.getEmail(), verificationId);
        adminVerificationService.rejectVerification(verificationId);

        return ResponseEntity.ok("회원 이미지, 음성 검증이 거절되었습니다.");
    }

    /**
     * 부적절한 사진 강제 삭제
     */
    @PostMapping("/{userId}/images/{imageId}/force-delete")
    @AuditLog(action = "회원 사진 강제 삭제")
    public ResponseEntity<String> deleteUserImage(
            @PathVariable("userId") Long userId,
            @PathVariable("imageId") Long imageId,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} deleted image {} of user {}", admin.getEmail(), imageId, userId);
        adminVerificationService.deleteUserImageForcefully(userId, imageId);

        return ResponseEntity.ok("부적절한 사진이 삭제되었습니다.");
    }

    /**
     * 프로필 소개글 초기화
     */
    @PutMapping("/{userId}/introduce/reset")
    @AuditLog(action = "회원 소개글 초기화")
    public ResponseEntity<String> resetUserIntroduce(
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} reset introduce of user {}", admin.getEmail(), userId);
        adminVerificationService.resetUserIntroduce(userId);

        return ResponseEntity.ok("회원 소개글이 초기화 문구로 변경되었습니다.");
    }

    /**
     * 회원 음성 강제 삭제
     */
    @PostMapping("/{userId}/voice/force-delete")
    @AuditLog(action = "회원 음성 강제 삭제")
    public ResponseEntity<String> deleteUserVoice(
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal UserEntity admin) {
        log.warn("Admin {} deleted voice of user {}", admin.getEmail(), userId);
        adminVerificationService.deleteUserVoiceForcefully(userId);

        return ResponseEntity.ok("부적절한 음성 소개가 삭제되었습니다.");
    }

}
