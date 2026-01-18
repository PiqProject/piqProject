package piq.piqproject.domain.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.annotation.AuditLog;
import piq.piqproject.domain.admin.dto.request.AdminRefundRequestDto;
import piq.piqproject.domain.admin.service.AdminPaymentService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/payments")
@PreAuthorize("hasRole('ROLE_ADMIN')") // 관리자 권한 필수
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    /**
     * 결제 강제 환불 API
     * 관리자가 주문번호와 사유를 입력하여 강제로 환불을 수행합니다.
     */
    @PostMapping("/refund")
    @AuditLog(action = "결제 강제 환불")
    public ResponseEntity<String> refundPayment(
            @Valid @RequestBody AdminRefundRequestDto request,
            @AuthenticationPrincipal UserEntity admin) {

        log.info("Admin {} requested refund for merchantUid: {}. Reason: {}",
                admin.getEmail(), request.getMerchantUid(), request.getCancelReason());

        adminPaymentService.refundPaymentByMerchantUid(request);

        return ResponseEntity.ok("정상적으로 환불 및 포인트 회수 처리가 완료되었습니다.");
    }
}