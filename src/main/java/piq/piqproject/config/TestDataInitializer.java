package piq.piqproject.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.config.jwt.JwtTokenProvider;
import piq.piqproject.domain.matches.entity.MatchingEntity;
import piq.piqproject.domain.matches.enums.MatchingStatus;
import piq.piqproject.domain.matches.repository.MatchingRepository;
import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.userimages.repository.UserImageRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender;
import piq.piqproject.domain.users.enums.Role;
import piq.piqproject.domain.users.enums.SocialType;
import piq.piqproject.domain.users.repository.UserRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MatchingRepository matchingRepository;
    private final UserImageRepository userImageRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 이미 매칭 데이터가 25건 이상 있다면 중복 생성 방지
        if (matchingRepository.count() >= 25) {
            log.info("[TestDataInitializer] 매칭 데이터가 이미 충분히 존재합니다. (count >= 25)");
            userRepository.findByEmail("test@test.com").ifPresent(this::printJmeterGuide);
            return;
        }

        log.info("==================================================================");
        log.info(">>>> [TestDataInitializer] JMeter 테스트용 더미 데이터 생성 시작 <<<<");
        log.info("==================================================================");

        // 1. 기준이 될 Sender(발신자) 계정 생성 또는 조회
        UserEntity sender = userRepository.findByEmail("test@test.com")
                .orElseGet(() -> {
                    UserEntity newUser = UserEntity.builder()
                            .email("test@test.com")
                            .password(passwordEncoder.encode("12345678"))
                            .nickname("테스터")
                            .gender(Gender.MALE)
                            .age(26)
                            .mbti("ENFP")
                            .pqPoint(10000)
                            .isActive(true)
                            .isAppAlarm(false)
                            .isWebAlarm(false)
                            .socialType(SocialType.NONE)
                            .build();
                    newUser.addRole(Role.USER);
                    UserEntity savedUser = userRepository.save(newUser);

                    // 대표 이미지 등록 (N+1 및 default_batch_fetch_size 테스트용)
                    userImageRepository.save(UserImageEntity.builder()
                            .user(savedUser)
                            .imageUrl("https://example.com/profiles/sender.png")
                            .isMainImage(true)
                            .build());

                    return savedUser;
                });

        // 2. Receiver(수신자) 2000명 및 매칭 데이터 2000건 생성
        int createdCount = 0;
        for (int i = 1; i <= 2000; i++) {
            String partnerEmail = "partner" + i + "@test.com";

            final int index = i;
            UserEntity receiver = userRepository.findByEmail(partnerEmail)
                    .orElseGet(() -> {
                        UserEntity newReceiver = UserEntity.builder()
                                .email(partnerEmail)
                                .password(passwordEncoder.encode("12345678"))
                                .nickname("상대방" + index)
                                .gender(Gender.FEMALE)
                                .age(24)
                                .mbti("INFJ")
                                .pqPoint(5000)
                                .isActive(true)
                                .isAppAlarm(false)
                                .isWebAlarm(false)
                                .socialType(SocialType.NONE)
                                .build();
                        newReceiver.addRole(Role.USER);
                        UserEntity savedReceiver = userRepository.save(newReceiver);

                        // 각 상대방 프로필 대표 이미지 등록
                        userImageRepository.save(UserImageEntity.builder()
                                .user(savedReceiver)
                                .imageUrl("https://example.com/profiles/partner" + index + ".png")
                                .isMainImage(true)
                                .build());

                        return savedReceiver;
                    });

            // 이미 동일한 매칭이 없으면 매칭 생성
            if (matchingRepository.findBySenderIdAndReceiverId(sender.getId(), receiver.getId()).isEmpty()) {
                MatchingEntity match = MatchingEntity.builder()
                        .sender(sender)
                        .receiver(receiver)
                        .status(MatchingStatus.PENDING)
                        .senderUsedPoints(10)
                        .receiverUsedPoints(10)
                        .message("N+1 성능 테스트용 매칭 메시지 " + i)
                        .build();
                matchingRepository.save(match);
                createdCount++;
            }
        }

        log.info(">>>> [TestDataInitializer] 더미 매칭 데이터 {}건 생성 완료!", createdCount);
        printJmeterGuide(sender);
    }

    private void printJmeterGuide(UserEntity sender) {
        String token = jwtTokenProvider.createAccessToken(sender);
        System.out.println("\n==================================================================");
        System.out.println("  [JMeter 부하 테스트 안내]");
        System.out.println("  - 테스트 URL: http://localhost:8080/api/v1/matches/sent?page=0&size=20");
        System.out.println("  - HTTP Method: GET");
        System.out.println("  - HTTP Header: Authorization");
        System.out.println("  - Header Value: Bearer " + token);
        System.out.println("==================================================================\n");
    }
}
