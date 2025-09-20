package piq.piqproject.common.error.exception;

public class InternalServerException extends CustomException {
    public InternalServerException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InternalServerException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
