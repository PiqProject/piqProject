package piq.piqproject.common.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.domain.users.entity.UserEntity;

@Aspect
@Component
@RequiredArgsConstructor
public class ActiveUserAspect {

    // @RequireActiveUser 어노테이션이 붙은 메서드가 실행되기 '이전(@Before)'에 이 로직을 수행하라
    @Before("@annotation(piq.piqproject.common.annotation.RequireActiveUser)")
    public void checkActiveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. 인증 객체 자체가 없는 경우 (방어 코드)
        if (authentication == null) {
            throw new InternalServerException(ErrorCode.MALFORMED_JWT_TOKEN, "Authentication not found");
        }

        Object principal = authentication.getPrincipal();

        // 2. 타입 체크로 한방에 해결!
        // 로그인 안 했으면 principal은 "anonymousUser"(String)이므로 이 if문을 통과 못 함 -> else로 감
        if (principal instanceof UserEntity user) {
            if (!user.getIsActive()) {
                throw new InternalServerException(ErrorCode.DISABLED_ACCOUNT_USER, "Account is not actived");
            }
        } else {
            // UserEntity가 아니라는 뜻은 -> 로그인 안 했거나(String(anonymousUser)), 이상한 객체라는 뜻
            throw new InternalServerException(ErrorCode.MALFORMED_JWT_TOKEN, "Login is required");
        }
    }
}