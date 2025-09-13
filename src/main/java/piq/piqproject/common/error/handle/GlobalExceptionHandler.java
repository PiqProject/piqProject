package piq.piqproject.common.error.handle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.swagger.v3.oas.annotations.Hidden;
import piq.piqproject.common.error.dto.ErrorDetailsDto;
import piq.piqproject.common.error.dto.ErrorResponseDto;
import piq.piqproject.common.error.dto.ValidErrorResponseDto;
import piq.piqproject.common.error.exception.CustomException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @RestControllerAdvice - 전역 예외 처리(Global Exception Handling)를 위한 컨트롤러 어드바이스
 *                       - @ControllerAdvice와 @ResponseBody를 결합한 어노테이션
 *                       ➡️ ExceptionHandler에 AOP를 적용시키며, 객체를 응답할 수 있도록 해줍니다.
 *                       <p>
 *                       애플리케이션 전반에서 발생하는 예외를 처리하고 그 결과를 HTTP 응답 본문(JSON)으로 반환할
 *                       수 있게 합니다.
 *                       이는 중복되는 예외 처리 코드를 줄이고 일관된 에러 응답 형식을 유지합니다.
 */

@Slf4j
@Hidden
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
                int statusCode = e.getErrorCode().getStatus().value();
                String code = e.getErrorCode().name();
                String message = e.getErrorCode().getMessage();

                log.error(
                                """
                                                CustomException occurred
                                                ------------------------
                                                status= {} ({})
                                                code= {}
                                                message= {}

                                                """,
                                status, // HTTP 상태 코드 값 (예: Conflict)
                                statusCode, // HTTP 상태 코드 값 (예: 409)
                                code, // ErrorCode의 이름 (예: ALREADY_EXISTS_USER)
                                message, // ErrorCode에 정의된 메시지 (예: "이미 가입된 유저입니다.)
                                e);

                return ResponseEntity.status(status).body(ErrorResponseDto.of(status, code, message));
        }

        // 유효성 검사에 대한 에러 처리
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ValidErrorResponseDto> MethodArgumentNotValidExceptionHandler(
                        MethodArgumentNotValidException e) {

                String status = HttpStatus.BAD_REQUEST.getReasonPhrase();
                int statusCode = e.getStatusCode().value();

                // validation 에러의 경우 여러개가 발생할 수 있습니다.
                // BindingResult를 통해 필드별 에러 목록을 가져옵니다.
                List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();

                // 각 FieldError를 ErrorDetailDto로 매핑하여 필드명과 에러 메시지를 구조화합니다. ("어떤 필드에서", "어떤 오류
                // 메시지"가 발생했는지)
                List<ErrorDetailsDto> errorDetails = fieldErrors.stream().map(error -> {
                        return ErrorDetailsDto.of(error.getField(), error.getDefaultMessage());
                }).toList();

                // 로그에 출력할 에러 상세 목록을 포맷팅합니다.
                String formattedErrorDetails = errorDetails.stream()
                                .map(detail -> String.format("    - Field: %-15s Message: %s", detail.getField(),
                                                detail.getMessage()))
                                .collect(Collectors.joining("\n")); // 각 항목을 줄바꿈 문자로 연결

                log.error(
                                """
                                                Validation error occurred (MethodArgumentNotValidException)
                                                -----------------------------------------------------------
                                                status={} ({})
                                                Errors=
                                                {}
                                                """,
                                status, // HTTP 상태 코드 값 (예: BadRequest)
                                statusCode, // HTTP 상태 코드 값 (예: 400)
                                formattedErrorDetails, // 발생한 모든 에러 상세 정보
                                e);

                return ResponseEntity.status(statusCode)
                                .body(ValidErrorResponseDto.of(HttpStatus.valueOf("BAD_REQUEST"), errorDetails));
        }
}
