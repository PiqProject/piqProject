package piq.piqproject.domain.payments.inapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.payments.common.enums.PaymentType;
import piq.piqproject.domain.payments.inapp.strategy.InAppPaymentResult;
import piq.piqproject.domain.payments.inapp.strategy.InAppPaymentStrategy;
import piq.piqproject.domain.users.entity.UserEntity;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InAppPaymentService {

    private final Map<PaymentType, InAppPaymentStrategy> strategies;

    // spring 실행시 InAppPaymentService의 구현체들을 모은 List를 할당
    public InAppPaymentService(List<InAppPaymentStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        InAppPaymentStrategy::getPaymentType,
                        Function.identity()));
    }

    /**
     * 특정 결제 수단 전략을 사용하여 결제를 처리합니다.
     */
    public InAppPaymentResult processPayment(PaymentType type, Object request, UserEntity user) {
        InAppPaymentStrategy strategy = strategies.get(type);
        if (strategy == null) {
            log.error("지원하지 않는 결제 타입입니다: {}", type);
            throw new NotFoundException(ErrorCode.NOT_FOUND, "Unsupported payment type: " + type);
        }
        return strategy.verifyAndProcessPayment(request, user);
    }
}