package piq.piqproject.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 활성화(isActive = true)된 유저만 접근 가능하도록 제한하는 어노테이션
 * 비활성화(정지, 탈퇴 등)된 유저가 접근 시 예외를 발생시킴
 */
@Target(ElementType.METHOD) // 메서드 위에 붙임
@Retention(RetentionPolicy.RUNTIME) // 실행 중(Runtime)에도 동작해야 함
public @interface RequireActiveUser {
}