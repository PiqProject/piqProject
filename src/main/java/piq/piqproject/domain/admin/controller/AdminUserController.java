package piq.piqproject.domain.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import piq.piqproject.common.annotation.AuditLog;
import piq.piqproject.domain.admin.dto.request.BulkPointRequestDto;
import piq.piqproject.domain.admin.dto.request.PointRequestDto;
import piq.piqproject.domain.admin.dto.request.UserStatusRequestDto;
import piq.piqproject.domain.admin.dto.response.UserAdminDetailResponseDto;
import piq.piqproject.domain.admin.dto.response.UserAdminResponseDto;
import piq.piqproject.domain.admin.service.AdminUserService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 회원 목록 조회 (검색 가능)
     */
    @GetMapping
    @AuditLog(action = "회원 목록 조회")
    public ResponseEntity<Page<UserAdminResponseDto>> getUsers(
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} accessed user list. Keyword: {}", admin.getEmail(), keyword);
        return ResponseEntity.ok(adminUserService.getUsers(keyword, pageable));
    }

    /**
     * 회원 상세 조회
     */
    @GetMapping("/{userId}")
    @AuditLog(action = "회원 상세 조회")
    public ResponseEntity<UserAdminDetailResponseDto> getUserDetail(
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} accessed detail of user {}", admin.getEmail(), userId);
        return ResponseEntity.ok(adminUserService.getUserDetail(userId));
    }

    /**
     * 회원 상태 변경 (정지/해제)
     */
    @PutMapping("/{userId}/status")
    @AuditLog(action = "회원 상태 변경")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UserStatusRequestDto requestDto,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} changed status of user {} to {}", admin.getEmail(), userId, requestDto.getIsActive());
        adminUserService.updateUserStatus(userId, requestDto);
        return ResponseEntity.ok("회원 상태가 변경되었습니다.");
    }

    /**
     * 포인트 수동 조정
     */
    @PostMapping("/{userId}/point")
    @AuditLog(action = "포인트 수동 조정")
    public ResponseEntity<String> adjustPoint(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody PointRequestDto requestDto,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} adjusted point of user {}. Amount: {}, Reason: {}",
                admin.getEmail(), userId, requestDto.getAmount(), requestDto.getReason());

        adminUserService.adjustPoint(userId, requestDto);
        return ResponseEntity.ok("포인트가 조정되었습니다.");
    }

    /**
     * 전회원 또는 성별별 포인트 일괄 조정
     */
    @PostMapping("/bulk-point")
    @AuditLog(action = "포인트 일괄 조정")
    public ResponseEntity<String> adjustBulkPoint(
            @Valid @RequestBody BulkPointRequestDto requestDto,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} initiated bulk point adjustment. TargetGender: {}, Amount: {}, Reason: {}",
                admin.getEmail(), requestDto.getTargetGender(), requestDto.getAmount(), requestDto.getReason());

        adminUserService.adjustBulkPoint(requestDto);
        return ResponseEntity.ok("포인트 일괄 조정이 완료되었습니다.");
    }

    /**
     * 회원 강제 탈퇴
     * DB 데이터와 S3 파일을 모두 영구 삭제합니다.
     */
    @PostMapping("/{userId}/force-delete")
    @AuditLog(action = "회원 강제 탈퇴")
    public ResponseEntity<String> deleteUser(
            @PathVariable("userId") Long userId,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} deleted user {}", admin.getEmail(), userId);
        adminUserService.deleteUserForcefully(userId);

        return ResponseEntity.ok("회원이 강제 탈퇴 처리되었습니다.");
    }

}