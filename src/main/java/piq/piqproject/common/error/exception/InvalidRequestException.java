package piq.piqproject.common.error.exception;

public class InvalidRequestException extends CustomException {
    public InvalidRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
