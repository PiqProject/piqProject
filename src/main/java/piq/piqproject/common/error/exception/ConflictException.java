package piq.piqproject.common.error.exception;

/**
 * 정의해두었던 CustomException을 상속받습니다.
 * 이를 통해 ConflictException이 발생하면 자동으로 CustomException을 통해 에러 처리가 가능해집니다.
 */
public class ConflictException extends CustomException {
    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }
}
