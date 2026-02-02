package piq.piqproject.domain.payments.inapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import piq.piqproject.domain.payments.common.enums.PaymentType;
import piq.piqproject.domain.payments.inapp.dto.request.ApplePaymentRequestDto;
import piq.piqproject.domain.payments.inapp.dto.request.GooglePaymentRequestDto;
import piq.piqproject.domain.payments.inapp.service.InAppPaymentService;
import piq.piqproject.domain.payments.inapp.strategy.InAppPaymentResult;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequestMapping("/api/payments/inapp")
@RequiredArgsConstructor
public class InAppPaymentController {

    private final InAppPaymentService inAppPaymentService;

    /**
     * Google Play Store 결제 검증 및 처리
     */
    @PostMapping("/google/verify")
    public ResponseEntity<InAppPaymentResult> verifyGoogle(@AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody GooglePaymentRequestDto request) {
        InAppPaymentResult result = inAppPaymentService.processPayment(PaymentType.GOOGLE, request, user);
        return ResponseEntity.ok(result);
    }

    /**
     * Apple App Store 결제 검증 및 처리
     */
    @PostMapping("/apple/verify")
    public ResponseEntity<InAppPaymentResult> verifyApple(@AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody ApplePaymentRequestDto request) {
        InAppPaymentResult result = inAppPaymentService.processPayment(PaymentType.APPLE, request, user);
        return ResponseEntity.ok(result);
    }
}
