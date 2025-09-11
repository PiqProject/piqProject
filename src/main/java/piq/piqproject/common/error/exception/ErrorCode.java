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
    JWT_TOKEN_MISSING(HttpStatus.BAD_REQUEST, "JWT 토큰이 제공되지 않았거나 유효하지 않습니다."),

    // UNAUTHORIZED (401) : 인증되지 않은 접근,
    JWT_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "JWT 토큰이 만료되었습니다."),
    UNSUPPORTED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "지원되지 않는 JWT 토큰 형식입니다."),
    MALFORMED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "손상되었거나 올바르지 않은 형식의 JWT 토큰입니다."),
    INVALID_JWT_SIGNATURE(HttpStatus.UNAUTHORIZED, "유효하지 않은 JWT 서명입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다. 다시 로그인해주세요."),

    // FORBIDDEN (403) : 권한 없는 접근,
    DISABLED_ACCOUNT_USER(HttpStatus.FORBIDDEN, "비활성화된 계정입니다."),
    NOT_REVIEW_OWNER(HttpStatus.FORBIDDEN, "자신의 리뷰만 수정 및 삭제 가능합니다."),

    // NOT_FOUND (404) : 찾을 수 없음
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "가입되지 않은 이메일입니다."),
    NOT_FOUND_REFRESH_TOKEN(HttpStatus.NOT_FOUND, "리프레시 토큰이 존재하지 않습니다. 다시 로그인해주세요."),
    NOT_FOUND_REVIEW(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),

    // CONFLICT (409) : 충돌
    ALREADY_EXISTS_USER(HttpStatus.CONFLICT, "이미 가입된 유저입니다."),
    ALREADY_EXISTS_REVIEW(HttpStatus.CONFLICT, "서비스에 대한 리뷰는 1회만 가능합니다."),

    // INTERNAL_SERVER_ERROR (500) : 서버 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 오류가 발생했습니다."),
    JWT_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JWT 처리 중 예상치 못한 오류가 발생했습니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
