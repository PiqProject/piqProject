package piq.piqproject.common.error.handle;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.dto.ErrorDetailsDto;
import piq.piqproject.common.error.dto.ErrorResponseDto;
import piq.piqproject.common.error.dto.ValidErrorResponseDto;
import piq.piqproject.common.error.exception.CustomException;
import piq.piqproject.common.error.exception.ErrorCode;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * @RestControllerAdvice - 전역 예외 처리(Global Exception Handling)를 위한 컨트롤러 어드바이스
 *                       - @ControllerAdvice와 @ResponseBody를 결합한 어노테이션
 *                       ➡️ ExceptionHandler에 AOP를 적용시키며, 객체를 응답할 수 있도록 해줍니다.
 *                       애플리케이션 전반에서 발생하는 예외를 처리하고 그 결과를 HTTP 응답 본문(JSON)으로 반환할
 *                       수 있게 합니다.
 *                       이는 중복되는 예외 처리 코드를 줄이고 일관된 에러 응답 형식을 유지합니다.
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
        public ResponseEntity<ErrorResponseDto> handleCustomExceptionHandler(CustomException e) {

                HttpStatus status = e.getErrorCode().getStatus();
                String code = e.getErrorCode().name();
                String message = e.getMessage();

                // logger에 e를 추가하면 스택 트레이스를 로깅함
                log.warn(
                                """
                                                CustomException occurred
                                                ------------------------
                                                status= {} ({})
                                                code= {}
                                                message= {}
                                                """,
                                status.getReasonPhrase(), status.value(), code, message, e);

                return ResponseEntity.status(status).body(ErrorResponseDto.of(status, code, message));
        }

        // 유효성 검사에 대한 에러 처리
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ValidErrorResponseDto> handleMethodArgumentNotValidExceptionHandler(
                        MethodArgumentNotValidException e) {

                HttpStatus status = HttpStatus.BAD_REQUEST; // HTTP 상태 코드 값 (예: BadRequest)

                // validation 에러의 경우 여러개가 발생할 수 있습니다.
                // BindingResult를 통해 필드별 에러 목록을 가져옵니다.
                List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();

                // 각 FieldError를 ErrorDetailDto로 매핑하여 필드명과 에러 메시지를 구조화합니다. ("어떤 필드에서", "어떤 오류
                // 메시지"가 발생했는지)
                List<ErrorDetailsDto> errorDetails = fieldErrors.stream()
                                .map(error -> {
                                        return ErrorDetailsDto.of(error.getField(), error.getDefaultMessage());
                                })
                                .toList();

                // 로그에 출력할 에러 상세 목록을 포맷팅합니다.
                String formattedErrorDetails = errorDetails.stream()
                                .map(detail -> String.format("    - Field: %-15s Message: %s", detail.getField(),
                                                detail.getMessage()))
                                .collect(Collectors.joining("\n")); // 각 항목을 줄바꿈 문자로 연결

                log.warn(
                                """
                                                Validation error occurred (MethodArgumentNotValidException)
                                                -----------------------------------------------------------
                                                status= {} ({})
                                                Errors=
                                                {}
                                                """,
                                status.getReasonPhrase(), status.value(), formattedErrorDetails, e);

                return ResponseEntity.status(status)
                                .body(ValidErrorResponseDto.of(status, errorDetails));
        }

        /**
         * 클라이언트가 요청한 HTTP 메서드가 특정 리소스에 대해 지원되지 않을 때 발생하는 예외를 처리합니다.
         * 응답: 405 Method Not Allowed
         */
        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ErrorResponseDto> handleHttpRequestMethodNotSupported(
                        HttpRequestMethodNotSupportedException e, HttpServletRequest request) { // request 파라미터 추가

                ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;

                log.warn(
                                """
                                                MethodNotSupportedException occurred
                                                ------------------------------------
                                                status= {} ({})
                                                message= {}
                                                requestMethod= {}
                                                """,
                                errorCode.getStatus().getReasonPhrase(), errorCode.getStatus().value(),
                                errorCode.getMessage(), request.getMethod(), e);

                // ErrorResponseDto 생성 및 반환
                return ResponseEntity.status(errorCode.getStatus())
                                .body(ErrorResponseDto.of(errorCode.getStatus(), errorCode.name(),
                                                errorCode.getMessage()));
        }

        /**
         * 업로드 파일 크기가 서버에서 설정한 최대치를 초과했을 때 발생하는 예외를 처리합니다.
         * 응답: 413 Payload Too Large
         *
         * @param e 발생한 MaxUploadSizeExceededException 예외
         * @return ErrorResponseDto 형식의 에러 응답
         */
        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ErrorResponseDto> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {

                // ErrorCode Enum에 PAYLOAD_TOO_LARGE 같은 코드를 정의해두면 더 좋습니다.
                // 여기서는 직접 상태와 메시지를 생성하겠습니다.
                ErrorCode errorCode = ErrorCode.FILE_SIZE_EXCEEDED; // ErrorCode에 이 항목을 추가했다고 가정합니다.

                HttpStatus status = errorCode.getStatus();
                String code = errorCode.name();
                String message = errorCode.getMessage();

                log.warn(
                                """
                                                MaxUploadSizeExceededException occurred
                                                ---------------------------------------
                                                status= {} ({})
                                                code= {}
                                                message= {}
                                                """,
                                status.getReasonPhrase(), status.value(), code, message, e);

                return ResponseEntity.status(status).body(ErrorResponseDto.of(status, code, message));
        }

        /**
         * JSON 파싱 실패 등 잘못된 요청 형식 처리
         * 클라이언트에서 잘못 보낸 것이므로 따로 빼서 처리
         * 응답: 400 Bad Request
         */
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException(
                        HttpMessageNotReadableException e) {
                ErrorCode errorCode = ErrorCode.BAD_REQUEST;

                HttpStatus status = errorCode.getStatus();
                String code = errorCode.name();
                String message = errorCode.getMessage();

                log.warn("""
                                HttpMessageNotReadableException occurred
                                ----------------------------------------
                                status= {} ({})
                                message= {}
                                """, status.getReasonPhrase(), status.value(), e.getMessage(), e);

                return ResponseEntity.status(status).body(ErrorResponseDto.of(status, code, message));
        }

        /**
         * 잘못된 URL 요청 시 (404 Not Found)
         * Spring Boot 3.2+부터 NoResourceFoundException이 발생합니다.
         */
        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ErrorResponseDto> handleNoResourceFoundException(NoResourceFoundException e) {
                // ErrorCode에 NOT_FOUND_RESOURCE 같은 게 있다면 사용, 없다면 직접 작성
                HttpStatus status = HttpStatus.NOT_FOUND;
                String code = "NOT_FOUND";
                String message = "요청한 리소스를 찾을 수 없습니다. URL을 확인해주세요.";

                log.warn("No resource found: {} {}", e.getHttpMethod(), e.getResourcePath());

                return ResponseEntity.status(status).body(ErrorResponseDto.of(status, code, message));
        }

        /**
         * [최상위 예외 핸들러]
         * 위에서 처리되지 않은 모든 예외(NullPointerException, IllegalArgumentException 등)를 처리합니다.
         * 보통 500 Internal Server Error로 처리합니다.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponseDto> handleAllException(Exception e) {

                // ErrorCode에 INTERNAL_SERVER_ERROR가 정의되어 있다고 가정
                ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

                HttpStatus status = errorCode.getStatus();
                String code = errorCode.name();
                String message = "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요." + e.getMessage();

                // 500 에러는 서버 문제이므로 warn 대신 error 레벨로 로깅하여 알람 등을 연동하기 좋게 합니다.
                log.error("""
                                Unhandled Exception occurred (Internal Server Error)
                                ----------------------------------------------------
                                status= {} ({})
                                code= {}
                                message= {}
                                exception= {}
                                """, status.getReasonPhrase(), status.value(), code, e.getMessage(),
                                e.getClass().getName(), e);

                return ResponseEntity.status(status).body(ErrorResponseDto.of(status, code, message));
        }

}
