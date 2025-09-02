package piq.piqproject.common.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 유효성 검사(Validation) 에러를 제외한 애플리케이션 내 모든 에러의 HTTP 상태 코드와 메시지를 관리합니다.
 * <p>
 * 에러는 유형별로 주석을 통해 분류하였으며, 해당 주석 아래에 세부 에러를 정의합니다.
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // BAD_REQUEST (400) : 잘못된 요청

    // UNAUTHORIZED (401) : 인증되지 않은 접근

    // FORBIDDEN (403) : 권한 없는 접근

    // NOT_FOUND (404) : 찾을 수 없음

    // CONFLICT (409) : 충돌
    ALREADY_EXISTS_USER(HttpStatus.CONFLICT, "이미 가입된 유저입니다."),

    // INTERNAL_SERVER_ERROR (500) : 서버 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 오류가 발생했습니다."),
    ;

    private final HttpStatus status;
    private final String message;
}


