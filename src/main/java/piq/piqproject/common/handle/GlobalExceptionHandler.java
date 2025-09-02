package piq.piqproject.common.handle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import piq.piqproject.common.CustomException;
import piq.piqproject.common.ErrorCode;
import piq.piqproject.common.dto.ErrorResponseDto;

/**
 * @RestControllerAdvice - 전역 예외 처리(Global Exception Handling)를 위한 컨트롤러 어드바이스
 * - @ControllerAdvice와 @ResponseBody를 결합한 어노테이션
 * ➡️ ExceptionHandler에 AOP를 적용시키며, 객체를 응답할 수 있도록 해줍니다.
 * <p>
 * 애플리케이션 전반에서 발생하는 예외를 처리하고 그 결과를 HTTP 응답 본문(JSON)으로 반환할 수 있게 합니다.
 * 이는 중복되는 예외 처리 코드를 줄이고 일관된 에러 응답 형식을 유지합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 하위 클래스에서 발생하는 모든 예외를 가로채 처리합니다.
     *
     * @ExceptionHandler 어노테이션을 사용하면 CustomException가 발생했을 때 이 메서드가 실행됩니다.
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponseDto> CustomExceptionHandler(CustomException e) {

        HttpStatus status = e.getErrorCode().getStatus();
        String code = e.getErrorCode().name();
        String message = e.getErrorCode().getMessage();

        log.error("CustomException occurred:\n" +
                        "  status={}\n" +
                        "  code={}\n" +
                        "  message={}",
                status,  // HTTP 상태 코드 값 (예: 409)
                code,    // ErrorCode의 이름 (예: ALREADY_EXISTS_USER)
                message,  // ErrorCode에 정의된 메시지 (예: "이미 가입된 유저입니다.)
                e
        );

        return ResponseEntity
                .status(status)
                .body(ErrorResponseDto.of(status, code, message));
    }
}
