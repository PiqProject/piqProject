package piq.piqproject.domain.users.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.domain.users.repository.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {
    private final UserService userService;
    private final UserRepository userRepository;

    @Value("${admin.account.creation.enabled:false}") // 기본값은 false로, 실수로 활성화되는 것을 방지
    private boolean adminCreationEnabled;

    @Value("${admin.initial.email:}")
    private String adminEmail;

    @Value("${admin.initial.password:}") // 실제 서비스에서는 암호화된 값을 사용
    private String adminPassword;

    @PostConstruct // 애플리케이션 시작 후 실행
    public void initializeAdminAccount() {
        log.info("" + adminPassword);
        if (!adminCreationEnabled) {
            // 초기화 기능이 비활성화되어 있으면 아무것도 하지 않음
            return;
        }

        // 필수 정보가 모두 설정되어 있는지 확인
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            System.err.println("초기 관리자 계정 생성을 위한 설정 정보가 부족합니다. (admin.initial.email, admin.initial.password)");
            return;
        }

        // 이미 관리자 계정이 존재하는지 확인
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            System.out.println("이미 관리자 계정이 존재합니다.");
            return;
        }

        // 관리자 계정 생성 (비밀번호는 암호화되어야 함)
        try {
            userService.createAdminAccount(adminEmail, adminPassword);
            System.out.println("초기 관리자 계정이 생성되었습니다: " + adminEmail);
        } catch (Exception e) {
            System.err.println("초기 관리자 계정 생성에 실패했습니다: " + e.getMessage());
        }
    }
}