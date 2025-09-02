package piq.piqproject.common;

import lombok.Getter;

/**
 * 애플리케이션 내에서 발생하는 모든 예외의 최상위 추상화 클래스입니다.
 * <p>
 * RuntimeException을 상속받아 Unchecked Exception으로 동작합니다.
 * 특정 상황에 대한 상세 정보를 포함하는 ErrorCode를 필드로 가집니다.
 * <p>
 * CustomException 사용 이유
 * <p>
 * - 코드 일관성: 애플리케이션 전반에 걸쳐 예외 처리 방식을 통일합니다.
 * - 중복 코드 제거: GlobalExceptionHandler에서 CustomException만 가로채면 모든 하위 예외를 일괄적으로 처리 가능합니다.
 * ➡️ 각 예외 유형별로 핸들러 코드를 작성할 필요 ❌
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
