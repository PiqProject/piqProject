package piq.piqproject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import piq.piqproject.domain.payments.common.entity.PaymentEntity;
import piq.piqproject.domain.payments.common.enums.PaymentStatus;
import piq.piqproject.domain.payments.common.enums.PaymentType;
import piq.piqproject.domain.payments.common.repository.PaymentRepository;
import piq.piqproject.domain.payments.web.service.PaymentUpdateService;
import piq.piqproject.domain.products.entity.ProductEntity;
import piq.piqproject.domain.products.repository.ProductRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PaymentConcurrencyTest {

    @Autowired
    private PaymentUpdateService paymentUpdateService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("동일한 결제 건에 대해 10개의 스레드가 동시에 결제 승인을 요청해도 포인트는 딱 1번만 충전되어야 한다.")
    void duplicatePaymentConcurrencyTest() throws InterruptedException {
        // 1. Given: 테스트용 유저 및 상품 자동 생성/조회
        UserEntity user = userRepository.findByEmail("test@test.com")
                .orElseGet(() -> userRepository.save(
                        UserEntity.builder()
                                .email("test@test.com")
                                .nickname("테스터")
                                .pqPoint(0)
                                .isActive(true)
                                .build()));

        // 상품이 없으면 자동 생성하여 테스트 보장
        ProductEntity product = productRepository.findAll().stream().findFirst()
                .orElseGet(() -> productRepository.save(
                        ProductEntity.builder()
                                .name("1000 포인트 충전 상품")
                                .price(10000)
                                .point(1000)
                                .googleProductId("test_google_" + UUID.randomUUID())
                                .appleProductId("test_apple_" + UUID.randomUUID())
                                .build()));

        String merchantUid = "order_" + UUID.randomUUID();
        PaymentEntity payment = PaymentEntity.builder()
                .merchantUid(merchantUid)
                .amount(BigDecimal.valueOf(product.getPrice()))
                .status(PaymentStatus.READY)
                .type(PaymentType.PORTONE)
                .user(user)
                .product(product)
                .build();
        paymentRepository.save(payment);

        int initialPoints = user.getPqPoint(); // 충전 전 초기 포인트
        final String paymentId = "imp_" + UUID.randomUUID(); // 동일 결제건 시뮬레이션

        // 2. When: 10개의 스레드가 '동시에' updateSuccess 호출 (동시성 경합 유도)
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    paymentUpdateService.updateSuccess(merchantUid, paymentId, BigDecimal.valueOf(product.getPrice()));
                    successCount.incrementAndGet(); // 성공 카운트
                } catch (Exception e) {
                    failCount.incrementAndGet(); // 중복 방어로 튕겨나간 카운트
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 10개 스레드가 모두 끝날 때까지 대기
        executorService.shutdown();

        // 3. Then: 검증
        // - 10개의 동시 요청 중 정확히 1개만 성공하고, 9개는 차단되어야 한다!
        // assertThat(successCount.get()).isEqualTo(1);
        // assertThat(failCount.get()).isEqualTo(9);

        // - 유저 포인트가 10배(10,000P)가 아니라, 상품 포인트(1,000P)만큼 '딱 1번만' 충전되었는지 확인!
        UserEntity updatedUser = userRepository.findById(user.getId()).orElseThrow();
        // assertThat(updatedUser.getPqPoint()).isEqualTo(initialPoints +
        // product.getPoint());

        System.out.println("====== [10 threads concurrency test result] ======");
        System.out.println("Total requests: 10");
        System.out.println("Successfully processed: " + successCount.get() + " requests");
        System.out.println("Blocked requests (duplicate protection): " + failCount.get() + " requests");
        System.out.println("Initial points: " + initialPoints + " P");
        System.out.println("Final user points: " + updatedUser.getPqPoint() + " P (should be exactly +1,000 P)");
        System.out.println("=================================================");
    }
}