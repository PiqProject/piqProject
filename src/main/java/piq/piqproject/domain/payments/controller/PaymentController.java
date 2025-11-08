package piq.piqproject.domain.payments.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import piq.piqproject.domain.payments.dto.Request.PaymentCancelRequestDto;
import piq.piqproject.domain.payments.dto.Request.PaymentPrepareRequestDto;
import piq.piqproject.domain.payments.dto.Request.PaymentVerificationRequestDto;
import piq.piqproject.domain.payments.dto.Response.PaymentHistoryResponseDto;
import piq.piqproject.domain.payments.dto.Response.PaymentPrepareResponseDto;
import piq.piqproject.domain.payments.service.PaymentService;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 만들어줍니다.
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 프론트엔드에서 결제이전에 결제정보를 전달
     * 
     * @param request impUid, amount, email이 담긴 DTO
     * @return 결제 정보
     */
    @PostMapping("/prepare-payment")
    public ResponseEntity<PaymentPrepareResponseDto> preparePayment(@AuthenticationPrincipal UserEntity user,
            @RequestBody PaymentPrepareRequestDto request) {
        // 서비스 레이어에 사전 등록 로직 처리를 위임
        String merchantUid = paymentService.preparePayment(user, request);
        return ResponseEntity.ok(new PaymentPrepareResponseDto(merchantUid));
    }

    /**
     * 프론트엔드에서 결제 이후 결제정보를 전달받고 실제로 결제가 되었는지 검증
     *
     * @param request impUid, merchantUid가 담긴 DTO
     * @return 결제 검증 결과
     */
    @PostMapping("/verify")
    public ResponseEntity<String> verifyPayment(@RequestBody PaymentVerificationRequestDto request) {
        paymentService.verifyPayment(request);
        return ResponseEntity.ok("Payment verification successful.");
    }

    /**
     * 결제완료 후 취소 요청 처리 API
     *
     * @param request impUid, reason이 담긴 DTO
     * @return 결제 취소 결과
     */
    @PostMapping("/cancel")
    public ResponseEntity<String> cancelPayment(
            @AuthenticationPrincipal UserEntity user,
            @RequestBody PaymentCancelRequestDto request) {

        paymentService.cancelPayment(request, user);
        return ResponseEntity.ok("Payment cancellation successful.");
    }

    /**
     * 자신의 결제 내역을 페이징하여 조회하는 API
     * 
     * @param user     현재 로그인한 사용자 정보 (Spring Security가 주입)
     * @param pageable 페이징 정보 (예:
     *                 /api/v1/payments?page=0&size=10&sort=createdAt,desc)
     * @return 페이징된 결제 내역
     */
    @GetMapping("/all")
    public ResponseEntity<Page<PaymentHistoryResponseDto>> getMyPaymentHistory(
            @AuthenticationPrincipal UserEntity user,
            Pageable pageable) {

        Page<PaymentHistoryResponseDto> history = paymentService.getMyPaymentHistory(user, pageable);
        return ResponseEntity.ok(history);
    }
}