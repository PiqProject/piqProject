package piq.piqproject.domain.payments.common.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import piq.piqproject.domain.payments.common.dto.response.PaymentHistoryResponseDto;
import piq.piqproject.domain.payments.common.service.PaymentService;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService; // 공통 서비스

    // 통합 결제 내역 조회
    @GetMapping("/history")
    public ResponseEntity<Page<PaymentHistoryResponseDto>> getMyPaymentHistory(
            @AuthenticationPrincipal UserEntity user,
            Pageable pageable) {

        // 이 DTO는 payments/dto/response 에 있는 공통 DTO임
        return ResponseEntity.ok(paymentService.getMyPaymentHistory(user, pageable));
    }
}