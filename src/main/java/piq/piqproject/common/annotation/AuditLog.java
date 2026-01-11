package piq.piqproject.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 메서드 위에 붙임
@Retention(RetentionPolicy.RUNTIME) // 런타임까지 유지
public @interface AuditLog {

    // 행위의 종류 (예: "회원 상세 조회", "강제 탈퇴")
    String action();
}