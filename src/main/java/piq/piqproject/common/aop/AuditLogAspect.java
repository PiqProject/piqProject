package piq.piqproject.common.aop;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.annotation.AuditLog;
import piq.piqproject.common.util.IpUtil;
import piq.piqproject.domain.admin.log.entity.AdminAccessLogEntity;
import piq.piqproject.domain.admin.log.repository.AdminAccessLogRepository;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@Aspect // AOP 클래스임을 명시
@Component // Bean으로 등록
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AdminAccessLogRepository adminAccessLogRepository;

    /**
     * @AuditLog 어노테이션이 붙은 메서드가 '정상적으로' 리턴된 후에 실행됩니다.
     *           (예외 발생 시에는 기록하지 않음 -> 예외는 ErrorLog가 담당)
     */
    @AfterReturning(pointcut = "@annotation(auditLog)", returning = "result")
    public void doAuditLog(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        try {
            // 1. Request 정보 가져오기
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes == null)
                return;
            HttpServletRequest request = attributes.getRequest();

            String ip = IpUtil.getClientIp(request);
            String requestUrl = request.getRequestURI();
            String httpMethod = request.getMethod();

            // 2. 관리자 정보 가져오기 (Security Context)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long adminId = null;
            String adminEmail = "Anonymous";

            if (authentication != null && authentication.getPrincipal() instanceof UserEntity) {
                UserEntity admin = (UserEntity) authentication.getPrincipal();
                adminId = admin.getId();
                adminEmail = admin.getEmail();
            }

            // 3. 수행한 작업 내용 (어노테이션에 적은 값)
            String action = auditLog.action(); // 예: "회원 강제 탈퇴"

            // 4. 타겟 정보 (메서드 파라미터 -> 문자열로 변환)
            // 예: userId=5, reason="욕설"
            String target = Arrays.toString(joinPoint.getArgs());
            // (주의: 비밀번호 등 민감정보가 파라미터에 있다면 여기서 필터링 로직 추가 필요)

            // 5. DB 저장
            AdminAccessLogEntity logEntity = AdminAccessLogEntity.builder()
                    .adminId(adminId)
                    .adminEmail(adminEmail)
                    .ip(ip)
                    .httpMethod(httpMethod)
                    .url(requestUrl)
                    .action(action) // "회원 강제 탈퇴"
                    .target(target) // "[5, BanRequestDto(...)]"
                    .build();

            adminAccessLogRepository.save(logEntity);

            log.info("[AUDIT] Admin:{} Action:{} Target:{}", adminEmail, action, target);

        } catch (Exception e) {
            // 로그 저장이 실패했다고 해서 비즈니스 로직(회원 탈퇴 등)까지 롤백되면 안 됨.
            // 에러 로그만 남기고 넘어가야 함.
            log.error("감사 로그 저장 중 오류 발생", e);
        }
    }
}